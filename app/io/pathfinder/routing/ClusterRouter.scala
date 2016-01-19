package io.pathfinder.routing

import akka.actor.{ActorRef, Props}
import akka.event.{ActorEventBus, LookupClassification}
import com.avaje.ebean.Model
import io.pathfinder.models.{ModelId, Commodity, Vehicle, Cluster}
import io.pathfinder.routing.Action.{DropOff, PickUp}
import io.pathfinder.routing.ClusterRouter.ClusterRouterMessage.{RouteRequest, ClusterEvent}
import io.pathfinder.websockets.WebSocketMessage.{Error, Routed}
import io.pathfinder.websockets.pushing.EventBusActor
import io.pathfinder.websockets.{Events, ModelTypes}
import play.Logger
import play.api.Play
import play.api.libs.json.{JsResultException, JsString, JsArray, Reads, __, JsNumber, JsObject, Writes, JsValue}
import play.api.libs.ws.{WSResponse, WS}
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.functional.syntax._
import scala.language.postfixOps

import ClusterRouter._

import scala.util.{Failure, Success, Try}

object ClusterRouter {

    type Row = Array[Int]
    type Matrix = Array[Row]

    val routingServer = WS.url(Play.current.configuration.getString("routing.server").getOrElse(
        throw new scala.Error("routing.server not defined in application.conf, routing will not work")
    )).withHeaders(
        "Content-Type" -> "application/json",
        "Accept" -> "application/json"
    )

    def props(cluster: Cluster): Props = Props(new ClusterRouter(cluster.id))
    def props(clusterPath: String): Props = Props(new ClusterRouter(clusterPath))

    abstract sealed class ClusterRouterMessage

    object ClusterRouterMessage {

        /*** For when an item in the route is updated **/
        case class ClusterEvent(event: Events.Value, model: Model) extends ClusterRouterMessage

        /*** For when a client requests to see a specific route **/
        case class RouteRequest(client: ActorRef, id: ModelId) extends ClusterRouterMessage
    }

    object DistanceFinder {

        val googleMaps = WS.url("https://maps.googleapis.com/maps/api/distancematrix/json")
        val apiKey = Play.configuration.getString("google.key").orElse{
            Logger.warn("No API key set in application.conf file")
            None
        }

        def makeRequest(origins: TraversableOnce[(Double, Double)], dests: TraversableOnce[(Double, Double)]): Future[WSResponse] =
            apiKey.fold{
                googleMaps.withQueryString(
                    "origins" -> origins.map(latlng => f"(${latlng._1}%.4f,${latlng._2}%.4f)").mkString("|"),
                    "destinations" -> dests.map(latlng => f"(${latlng._1}%.4f,${latlng._2}%.4f)").mkString("|"))
            } { key =>
                googleMaps.withQueryString(
                    "origins" -> origins.map(latlng => f"(${latlng._1}%.4f,${latlng._2}%.4f)").mkString("|"),
                    "destinations" -> dests.map(latlng => f"(${latlng._1}%.4f,${latlng._2}%.4f)").mkString("|"),
                    "key" -> key)
            }.get()

        def parseResponse(res: WSResponse): Try[(Matrix,Matrix)] = Try(
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
            makeRequest(origins, dests).map{ resp =>
                Logger.info(resp.body)
                resp
            }.map(parseResponse).flatMap(
                _.recover{case t => return Future.failed(t)}.map(Future.successful).get
            )
    }
}

class ClusterRouter(clusterPath: String) extends EventBusActor with ActorEventBus with LookupClassification {

    private var cachedRoutes: Option[Seq[Route]] = None

    override type Event = (ModelId, Routed)
    override type Classifier = ModelId // subscribe by model and by id

    override protected def classify(event: Event): Classifier = event._1

    override protected def publish(event: Event, subscriber: ActorRef): Unit = {
        subscriber ! event._2
    }

    override def subscribe(client: ActorRef, c: Classifier): Boolean = {
        Logger.info("Websocket: "+ client+" subscribed to route updates for: "+c)
        super.subscribe(client, c)
    }

    def clusterRouted(routes: Seq[Route]): Routed = Routed(
        ModelTypes.Cluster,
        JsObject(Seq(("id",JsString(clusterPath)))),
        Writes.seq(Route.writes).writes(routes)
    )

    def vehicleRouted(route: Route): Routed = Routed(
        ModelTypes.Vehicle,
        Vehicle.format.writes(route.vehicle),
        Route.writes.writes(route)
    )

    def publish(routes: Seq[Route]): Unit ={
        publish((ModelId.ClusterPath(clusterPath), clusterRouted(routes))) // send list of routes to cluster subscribers
        routes.foreach { route =>
            val routeJson: JsValue = Route.writes.writes(route)
            val vehicleJson: JsValue = Vehicle.format.writes(route.vehicle)

            // publish vehicles to vehicle subscribers
            publish((ModelId.VehicleId(route.vehicle.id), Routed(ModelTypes.Vehicle, vehicleJson, routeJson)))
            route.actions.tail.collect {
                case PickUp(lat, lng, com) =>
                    val comJson = Commodity.format.writes(com) // publish commodities to commodity subscribers
                    publish((ModelId.CommodityId(com.id), Routed(ModelTypes.Commodity, comJson, routeJson)))
                case _Else => Unit
            }
        }
    }

    override protected def mapSize(): Int = 16

    /*
     * The cluster router just recalculates whenever a route request or update occurs
     */
    override def receive: Receive = {
        case ClusterEvent(event , model) => recalculate().map {
            res =>
                publish(res)
                cachedRoutes = Some(res)
        }
        case RouteRequest(client, mId) => cachedRoutes.map { routes =>
            Logger.info("using cached routes")
            Future.successful(routes)
        }.getOrElse(
            recalculate().map{ routes =>
                cachedRoutes = Some(routes)
                routes
            }
        ).recoverWith{
            case t =>
                client ! Error("Failed to route cluster: "+t.getMessage)
                Future.failed(t)
        }.foreach( routes =>
            mId match {
                case ModelId.ClusterPath(path) => client ! clusterRouted(routes)
                case ModelId.VehicleId(id) => routes.find(_.vehicle.id == id).foreach{ route =>
                    client ! vehicleRouted(route)
                }
                case ModelId.CommodityId(id) =>
                    var commodity: Commodity = null
                    routes.find{ route =>
                        route.actions.exists {
                            case PickUp(lat, lng, com) =>
                                if (com.id == id) {
                                    commodity = com
                                    true
                                } else false
                            case x => false
                        }
                    }.foreach{ route =>
                        client ! Routed(
                            ModelTypes.Commodity,
                            Commodity.format.writes(commodity),
                            Route.writes.writes(route)
                        )
                    }
            }
        )
        case _Else => super.receive(_Else)
    }

    val matrixWriter: Writes[Matrix] = Writes.arrayWrites[Row]//Json.writes[Array[Array[Double]]]

    def parseRoutingResponse(vehicles: Seq[Vehicle], commodities: Seq[Commodity], result: WSResponse): Try[Seq[Route]] =
        result.json.validate(
            (__ \ "routes").read(
                Reads.seq(Reads.seq(Reads.JsNumberReads.map(_.value.toInt))).map(routes =>
                    routes.map { arr =>
                        val routeBuilder = Route.newBuilder(vehicles(arr.head - 1 - 2 * commodities.size))
                        arr.tail.foreach( i =>
                            if (i <= commodities.size) {
                                routeBuilder += new PickUp(commodities(i - 1))
                            } else {
                                routeBuilder += new DropOff(commodities(i - commodities.size - 1))
                            }
                        )
                        routeBuilder.result()
                    }
                )
            )
        ).fold(
            { t => return Failure(JsResultException(t)) },
                //Logger.error("Failed to parse response json", new JsResultException(ex)); return Failure(ex) },
            routes => Success(routes)
        )

    private def recalculate(): Future[Seq[Route]] = {
        val cluster: Cluster = Cluster.Dao.read(clusterPath).getOrElse{
            Logger.warn("Cluster with id: "+clusterPath+" missing")
            return Future.failed(null)
        }
        Logger.info("RECALCULATING")
        val vehicles = cluster.descendants.map(_.vehicles).fold(cluster.vehicles)(_ ++ _)
        if (vehicles.size <= 0) {
            Logger.info("Someone asked Router to recalculate but there are no vehicles in cluster.")
            return Future.failed(null)
        }
        Logger.info("GOT VEHICLES")
        val commodities = cluster.descendants.map(_.commodities).fold(cluster.commodities)(_ ++ _)
        if (commodities.size <= 0) {
            Logger.info("someone asked for Router to recalculate but there are no commodities in cluster")
            return Future.failed(null)
        }
        Logger.info("GOT COMMODITIES")

        val starts = vehicles.map(x => (x.latitude, x.longitude))
        val pickups = commodities.map(c => (c.startLatitude, c.startLongitude))
        val dropOffs = commodities.map(c => (c.endLatitude, c.endLongitude))
        val body: Future[JsValue] = for {
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
                  _ ++ Seq.fill(commodities.size + vehicles.size)(0)
              )).map(row => JsArray(row.map(JsNumber(_))))
            )

            val comTable = JsObject(commodities.indices.map(num => ((num + 1).toString, JsNumber(num + commodities.size + 1))))
            val vehicleTable = JsArray(vehicles.indices.map(num => JsNumber(num + 2 * commodities.size + 1)))
            val capacities = JsObject(cluster.application.capacityParameters.map { p =>
                p.parameter -> JsObject(
                    commodities.zipWithIndex.map {
                        case (com, i) =>
                            (i + 1).toString ->
                                com.metadata.validate((__ \ p.parameter).read[JsNumber]).getOrElse(JsNumber(0))
                    } ++
                    vehicles.zipWithIndex.map {
                        case (veh, i) =>
                            (i + 1 + 2 * commodities.size).toString ->
                                veh.metadata.validate((__ \ p.parameter).read[JsNumber]).getOrElse(JsNumber(Integer.MAX_VALUE))
                    }
                )
            })
            Logger.info("Capacities: " + capacities)
            val parameters = JsObject(cluster.application.objectiveParameters.map { p =>
                p.parameter -> JsObject(
                    commodities.zipWithIndex.map {
                        case (com, i) =>
                            (i + 1).toString ->
                                com.metadata.validate((__ \ p.parameter).read[JsNumber]).getOrElse(JsNumber(0))
                    } ++
                    vehicles.zipWithIndex.map {
                        case (veh, i) =>
                            (i + 1 + 2 * commodities.size).toString ->
                                veh.metadata.validate((__ \ p.parameter).read[JsNumber]).getOrElse(JsNumber(Integer.MAX_VALUE))
                    }
                )
            })
            Logger.info("Parameters: " + parameters)
            JsObject(Seq(
                "commodities" -> comTable,
                "vehicles" -> vehicleTable,
                "capacities" -> capacities,
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
                "parameters" -> parameters,
                "objective" -> JsString(cluster.application.objectiveFunction.function)
            ))
        }
        body.recoverWith[JsValue]{
            case t =>
                Logger.error("Failed to request and receive route data from google maps api", t)
                Future.failed(t)
        }.flatMap[WSResponse]{ json =>
            Logger.info("Sending message: " + body)
            routingServer.post(
                json
            ).map{ response =>
               Logger.info("Received routing response: "+response.json)
               response
            }.recoverWith[WSResponse]{
                case t =>
                    Logger.warn("Failed to read route response", t)
                    Future.failed(t)
            }
        }.flatMap[Seq[Route]]{ resp =>
            parseRoutingResponse(vehicles, commodities, resp).map(Future.successful).recover{
                case t => Future.failed(t)
            }.get
        }
    }
}
