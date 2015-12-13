package io.pathfinder.routing

import akka.actor.{ActorRef, Props}
import akka.event.{ActorEventBus, LookupClassification}
import com.avaje.ebean.Model
import io.pathfinder.models.{Commodity, Vehicle, Cluster}
import io.pathfinder.routing.Action.{DropOff, PickUp}
import io.pathfinder.routing.ClusterRouter.ClusterRouterMessage.{RouteRequest, ClusterEvent}
import io.pathfinder.websockets.WebSocketMessage.Routed
import io.pathfinder.websockets.pushing.EventBusActor
import io.pathfinder.websockets.{Events, ModelTypes}
import play.Logger
import play.api.Play
import play.api.libs.json.{JsString, Json, JsArray, Reads, __, JsNumber, JsObject, Writes, JsValue}
import play.api.libs.ws.{WSResponse, WS}
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.functional.syntax._
import scala.language.postfixOps

import ClusterRouter._

import scala.util.Try

object ClusterRouter {

    type Row = Array[Int]
    type Matrix = Array[Row]

    private val routingServer = WS.url(Play.current.configuration.getString("routing.server").getOrElse(
        throw new Error("routing.server not defined in application.conf, routing will not work")
    )).withHeaders(
        "Content-Type" -> "application/json",
        "Accept" -> "application/json"
    )

    def props(cluster: Cluster): Props = Props(new ClusterRouter(cluster.id))
    abstract sealed class ClusterRouterMessage

    object ClusterRouterMessage {

        /*** For when an item in the route is updated **/
        case class ClusterEvent(event: Events.Value, model: Model) extends ClusterRouterMessage

        /*** For when a client requests to see a specific route **/
        case class RouteRequest(client: ActorRef, modelType: ModelTypes.Value, id: Long) extends ClusterRouterMessage
    }

    object DistanceFinder {

        private val googleMaps = WS.url("https://maps.googleapis.com/maps/api/distancematrix/json")

        private def makeRequest(origins: TraversableOnce[(Double, Double)], dests: TraversableOnce[(Double, Double)]): Future[WSResponse] =
            googleMaps.withQueryString(
                "origins" -> origins.map(latlng => f"(${latlng._1}%.4f,${latlng._2}%.4f)").mkString("|"),
                "destinations" -> dests.map(latlng => f"(${latlng._1}%.4f,${latlng._2}%.4f)").mkString("|")
            ).get()

        private def parseResponse(res: WSResponse): Try[(Matrix,Matrix)] = Try(
            res.json.validate(
                (__ \ "rows").read[Array[JsValue]].map(
                    _.map(_.validate(
                        (__ \ "elements").read[Array[JsValue]].map(
                            _.map(
                                _.validate(
                                    (__ \ "distance" \ "value").read[Int] and
                                    (__ \ "duration" \ "value").read[Int]
                                    tupled
                                ).get
                            ).unzip
                        )
                    ).get).unzip
                )
            ).get
        )

        def find(origins: Seq[(Double, Double)], dests: Seq[(Double, Double)]): Future[(Matrix,Matrix)] =
            makeRequest(origins, dests).map(parseResponse).flatMap(
                _.recover{case t => return Future.failed(t)}.map(Future.successful).get
            )
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

    def publish(routes: Seq[Route]): Unit ={
        val clusterRouted = Routed(
            ModelTypes.Cluster,
            JsObject(Seq(("id",JsNumber(clusterId)))),
            Writes.seq(Route.writes).writes(routes)
        )
        publish(((ModelTypes.Cluster,clusterId), clusterRouted)) // send list of routes to cluster subscribers
        routes.foreach { route =>
            val routeJson: JsValue = Route.writes.writes(route)
            val vehicleJson: JsValue = Vehicle.format.writes(route.vehicle)

            // publish vehicles to vehicle subscribers
            publish(((ModelTypes.Vehicle, route.vehicle.id), Routed(ModelTypes.Vehicle, vehicleJson, routeJson)))
            route.actions.tail.collect {
                case PickUp(lat, lng, com) =>
                    val comJson = Commodity.format.writes(com) // publish commodities to commodity subscribers
                    publish(((ModelTypes.Commodity, com.id), Routed(ModelTypes.Commodity, comJson, routeJson)))
                case _Else => Unit
            }
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

    val matrixWriter: Writes[Matrix] = Writes.arrayWrites[Row]//Json.writes[Array[Array[Double]]]

    private def recalculate(): Unit = {
        val cluster: Cluster = Cluster.Dao.read(clusterId).getOrElse{
            Logger.warn("Cluster with id: "+clusterId+" missing")
            return
        }
        Logger.info("RECALCULATING")
        val vehicles = cluster.descendants.map(_.vehicles).fold(cluster.vehicles)(_ ++ _)
        if (vehicles.size <= 0) {
            Logger.info("Someone asked Router to recalculate but there are no vehicles in cluster.")
            return
        }
        Logger.info("GOT VEHICLES")
        val commodities = cluster.descendants.map(_.commodities).fold(cluster.commodities)(_ ++ _)
        if (commodities.size <= 0) {
            Logger.info("someone asked for Router to recalculate but there are no commodities in cluster")
            return
        }
        Logger.info("GOT COMMODITIES")

        val starts = vehicles.map(x => (x.latitude, x.longitude))
        val pickups = commodities.map(c => (c.startLatitude, c.startLongitude))
        val dropOffs = commodities.map(c => (c.endLatitude, c.endLongitude))
        (for {
            (startToPickUpDist, startToPickUpDur) <- DistanceFinder.find(starts, pickups)
            (pickUpToDropOffDist, pickUpToDropOffDur) <- DistanceFinder.find(pickups, dropOffs)
            (pickUpToPickUpDist, pickUpToPickUpDur) <- DistanceFinder.find(pickups, pickups)
            (dropOffToPickUpDist, dropOffToPickUpDur) <- DistanceFinder.find(dropOffs, pickups)
            (dropOffToDropOffDist, dropOffToDropOffDur) <- DistanceFinder.find(dropOffs, dropOffs)
        } yield {
            def makeMatrix(
                startsToPickUps: Matrix,
                pickUpsToDropOffs: Matrix,
                pickUpsToPickUps: Matrix,
                dropOffsToPickUps: Matrix,
                dropOffsToDropOffs: Matrix
            ) = JsArray((
                    pickUpsToPickUps.zip(pickUpsToDropOffs).map(
                        tup => tup._1 ++ tup._2 ++ Seq.fill(vehicles.size)(0)
                    ) ++ dropOffsToPickUps.zip(dropOffsToDropOffs).map(
                        tup => tup._1 ++ tup._2 ++ Seq.fill(vehicles.size)(0)
                    ) ++ startsToPickUps.map(
                        _ ++ Seq.fill(commodities.size)(0) ++ Seq.fill(vehicles.size)(0)
                    )
                ).map(row => JsArray(row.map(JsNumber(_)))))

            val comTable = JsObject(commodities.indices.map(num => (num.toString,JsNumber(num+commodities.size))))
            val vehicleTable = JsArray(vehicles.indices.map(num => JsNumber(num+2*commodities.size)))
            val body = JsObject(Seq(
                "commodities" -> comTable,
                "vehicles" -> vehicleTable,
                "capacities" -> JsArray(),
                "distances" -> makeMatrix(
                    startToPickUpDist,
                    pickUpToDropOffDist,
                    pickUpToPickUpDist,
                    dropOffToPickUpDist,
                    dropOffToDropOffDist
                ),
                "durations" -> makeMatrix(
                    startToPickUpDur,
                    pickUpToDropOffDur,
                    pickUpToPickUpDur,
                    dropOffToPickUpDur,
                    dropOffToDropOffDur
                ),
                "vehicleParameters" -> JsArray(),
                "commodityParameters" -> JsArray(),
                "objective" -> JsString("0")
            ))
            Logger.info("Sending message: "+body)
            routingServer.post(
                body
            ).onComplete{ result =>
                val w = result.recover{
                    case t : Throwable =>
                        Logger.warn("Failed to read route response", t)
                        return
                }.get
                w.json.validate(
                    Reads.list(Reads.list(Reads.JsNumberReads.map(_.value.toInt))).map( routes =>
                        publish(routes.map{ arr =>
                            val routeBuilder = Route.newBuilder(vehicles(arr.head - 2 * commodities.size))
                            arr.tail.foreach(
                                i => if(i < commodities.size){
                                    routeBuilder += new PickUp(commodities(i))
                                } else {
                                    routeBuilder += new DropOff(commodities(i - commodities.size))
                                }
                            )
                            routeBuilder.result()
                        })
                    )
                )
            }
            Logger.info("SENDING DATA")
        }).onFailure{case t => Logger.error("Failed to request and receive route data from google maps api", t)}
    }
}
