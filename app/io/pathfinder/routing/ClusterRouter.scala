package io.pathfinder.routing

import akka.actor.{ActorRef, Props}
import akka.event.{ActorEventBus, LookupClassification}
import io.pathfinder.models.{Commodity, Vehicle, Cluster}
import io.pathfinder.routing.Action.{DropOff, PickUp, Start}
import io.pathfinder.routing.ClusterRouter.Recalculate
import io.pathfinder.websockets.WebSocketMessage.Routed
import io.pathfinder.websockets.pushing.EventBusActor
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage.Publish
import io.pathfinder.websockets.ModelTypes
import play.Logger
import play.api.libs.json.{JsNumber, JsObject, Writes, JsValue}

import scala.Function._
import scala.collection.mutable

object ClusterRouter {
    def props(cluster: Cluster): Props = Props(new ClusterRouter(cluster))
    case object Recalculate
}

class ClusterRouter(cluster: Cluster) extends EventBusActor with ActorEventBus with LookupClassification {
    override type Event = ((ModelTypes.Value, Long), Routed)
    override type Classifier = (ModelTypes.Value, Long) // subscribe by model and by id

    override protected def classify(event: Event): Classifier = event._1

    override protected def publish(event: Event, subscriber: ActorRef): Unit = {
        subscriber ! event._2
    }

    override def subscribe(client: ActorRef, c: Classifier): Boolean = {
        Logger.info("Websocket: "+ client+" subscribed to route updates for: "+c)
        super.subscribe(client, c)
    }

    def publish(route: Route): Unit ={
        val routeJson: JsValue = Route.writes.writes(route)
        val vehicleJson: JsValue = Vehicle.format.writes(route.vehicle)
        publish(((ModelTypes.Vehicle, route.vehicle.id), Routed(ModelTypes.Vehicle, vehicleJson, routeJson)))
        route.actions.collect {
            case PickUp(lat, lng, com) =>
                val comJson: JsValue = Commodity.format.writes(com)
                publish(((ModelTypes.Commodity, com.id), Routed(ModelTypes.Commodity, comJson, routeJson)))
            case _Else => Unit
        }
    }

    override protected def mapSize(): Int = 16

    override def receive: Receive = {
        case e: Either[_,_] => e.fold(self ! _, self ! _)
        case Recalculate => recalculate() // calling refresh will update the cluster model instance
        case _Else => super.receive(_Else)
    }

    private def recalculate(): Unit = {
        cluster.refresh()
        val vehicles = cluster.vehicles
        val commodities = cluster.commodities
        if (vehicles.size <= 0) {
            Logger.info("Someone asked Router to recalculate but there are no vehicles in cluster.")
            return
        }
        Logger.info("Reticulating splines")
        val builders: Seq[mutable.Builder[Action, Seq[Action]]] = vehicles.map { Seq.newBuilder[Action] += new Start(_)}
        var i = 0
        commodities.foreach {
            c => {
                Logger.info(String.format("Adding %s to a route", c))
                builders(i % vehicles.size) += new PickUp(c) += new DropOff(c)
                i += 1
            }
        }
        val routes: Seq[Route] = builders.zip(vehicles) map tupled { (builder, v) => new Route(v, builder.result()) }
        routes.foreach(publish)
        val clusterRouted = Routed(
            ModelTypes.Cluster,
            JsObject(Seq(("id",JsNumber(cluster.id)))),
            Writes.seq(Route.writes).writes(routes)
        )
        publish(((ModelTypes.Cluster,cluster.id), clusterRouted)) // send list of routes to cluster subscribers
        Logger.info("Finished recalculating routes")
    }
}
