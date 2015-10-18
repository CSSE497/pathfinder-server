package io.pathfinder.routing

import akka.actor.{ActorRef, Props}
import akka.event.{ActorEventBus, LookupClassification}
import io.pathfinder.models.Cluster
import io.pathfinder.routing.Action.{DropOff, PickUp, Start}
import io.pathfinder.websockets.WebSocketMessage.Routed
import io.pathfinder.websockets.pushing.EventBusActor
import io.pathfinder.websockets.{Events, ModelTypes}
import play.Logger
import play.api.libs.json.JsValue

import scala.Function._
import scala.collection.mutable

object ClusterRouter {
    def props(cluster: Cluster): Props = Props(new ClusterRouter(cluster))
}

class ClusterRouter(cluster: Cluster) extends EventBusActor with ActorEventBus with LookupClassification {
    override type Event = (ModelTypes.Value, Long, JsValue)
    override type Classifier = (ModelTypes.Value, Long) // subscribe by model and by id

    override protected def classify(event: Event): Classifier = (event._1, event._2)

    override protected def publish(event: Event, subscriber: ActorRef): Unit = {
        subscriber ! Routed(event._1, event._2, event._3)
    }

    def publish(route: Route): Unit ={
        val json: JsValue = Route.writes.writes(route)
        publish(ModelTypes.Vehicle, route.vehicle, json)
        publish(ModelTypes.Cluster, cluster.id, json)
        route.actions.collect {
            case PickUp(lat, lng, com) => publish(ModelTypes.Commodity, com.id, json)
            case _Else => Unit
        }
    }

    override protected def mapSize(): Int = 16

    override def receive: Receive = {
        case tup: (ModelTypes.Value, Events.Value, AnyRef) => recalculate() // calling refresh will update the cluster model instance
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
        val routes: Seq[Route] = builders.zip(vehicles) map tupled { (builder, v) => new Route(v.id, builder.result()) }
        routes.foreach(publish)
        Logger.info("Finished recalculating routes")
    }
}
