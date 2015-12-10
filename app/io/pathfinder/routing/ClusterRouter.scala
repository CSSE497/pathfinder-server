package io.pathfinder.routing

import akka.actor.{ActorRef, Props}
import akka.event.{ActorEventBus, LookupClassification}
import com.avaje.ebean.Model
import io.pathfinder.models.{Commodity, Vehicle, Cluster}
import io.pathfinder.routing.Action.{DropOff, PickUp, Start}
import io.pathfinder.routing.ClusterRouter.ClusterRouterMessage.{RouteRequest, ClusterEvent}
import io.pathfinder.websockets.WebSocketMessage.Routed
import io.pathfinder.websockets.pushing.EventBusActor
import io.pathfinder.websockets.{Events, ModelTypes}
import play.Logger
import play.api.libs.json.{JsNumber, JsObject, Writes, JsValue}

import dispatch.{Res, Req, url, Http}

import scala.concurrent.{Future, Promise}

import scala.Function._
import scala.collection.mutable

import ClusterRouter._

object ClusterRouter {
    def props(cluster: Cluster): Props = Props(new ClusterRouter(cluster.id))
    abstract sealed class ClusterRouterMessage

    private def googleMapsRequest(origins: TraversableOnce[(Double,Double)], dests: TraversableOnce[(Double,Double)]): Future[Res] =
        Http(url("https://maps.googleapis.com/maps/api/distancematrix/json?" +
            "origins=" + origins.map(latlng => String.format("%.4f,%.4f",latlng._1,latlng._2)).mkString("|") + "&" +
            "destinations=" + dests.map(latlng => String.format("%.4f,%.4f",latlng._1,latlng._2)).mkString("|")))

    private def parseRes(res: Res): Array[Double] = {
        print(res.getResponseBody)
        Array()
    }

    private def distances(origins: Seq[(Double,Double)], dests: Seq[(Double,Double)]): Future[Array[Array[Double]]] =
        Future.sequence(
            origins.map[Future[Array[Double]],Iterator[Future[Array[Double]]]](
                o => googleMapsRequest(Iterator.fill(dests.size)(o), dests).map(parseRes)
            )
        ).map(_.toArray)

    private def removeDiagonals(param: Array[Array[Double]]): Array[Array[Double]] = {
        (0 to param.length).foreach(
            i => param(i)(i) = -1
        )
        param
    }

    object ClusterRouterMessage {

        /*** For when an item in the route is updated **/
        case class ClusterEvent(event: Events.Value, model: Model) extends ClusterRouterMessage

        /*** For when a client requests to see a specific route **/
        case class RouteRequest(client: ActorRef, modelType: ModelTypes.Value, id: Long) extends ClusterRouterMessage
    }
}

class ClusterRouter(clusterId: Long) extends EventBusActor with ActorEventBus with LookupClassification {
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

    /*
     * The cluster router just recalculates whenever a route request or update occurs
     */
    override def receive: Receive = {
        case ClusterEvent(event , model) => recalculate()
        case RouteRequest(client, model, id) => recalculate()
        case _Else => super.receive(_Else)
    }

    private def recalculate(): Unit = {
        val cluster: Cluster = Cluster.Dao.read(clusterId).getOrElse{
            Logger.warn("Cluster with id: "+clusterId+" missing")
            return
        }
        val vehicles = cluster.descendants.map(_.vehicles).fold(cluster.vehicles)(_ ++ _)
        val commodities = cluster.descendants.map(_.commodities).fold(cluster.commodities)(_ ++ _)
        val vehicleIds = vehicles.map(_.id)
        val comIds = commodities.map(_.id)
        val starts = vehicles.map(x => (x.latitude, x.longitude))
        val pickups = commodities.map(c => (c.startLatitude, c.startLongitude))
        val dropOffs = commodities.map(c => (c.endLatitude, c.endLongitude))
        for {
            startsToPickups <- distances(starts, pickups)
            pickupsToDropOffs <- distances(pickups, dropOffs)
            pickUpsToPickUps <- distances(pickups, pickups).map(removeDiagonals)
            dropOffsToPickUps <- distances(dropOffs, pickups).map(removeDiagonals)
            dropOffsToDropOffs <- distances(dropOffs, dropOffs).map(removeDiagonals)
        } yield {
            // send to server
        }
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
        Logger.info("Finished recalculating routes:\n"+clusterRouted)
    }
}
