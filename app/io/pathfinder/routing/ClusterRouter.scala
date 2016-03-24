package io.pathfinder.routing

import akka.actor.{ActorRef, Props}
import akka.event.{ActorEventBus, LookupClassification}
import com.avaje.ebean.Model
import io.pathfinder.config.Global
import io.pathfinder.models.ModelId.ClusterPath
import io.pathfinder.models.{TransportStatus, CommodityStatus, ModelId, Commodity, Transport, Cluster}
import io.pathfinder.routing.Action.{DropOff, PickUp}
import io.pathfinder.routing.ClusterRouter.CacheState.{Updating, UpToDate}
import io.pathfinder.routing.ClusterRouter.ClusterRouterMessage.{Recalculate, RouteRequest, ClusterEvent}
import io.pathfinder.websockets.WebSocketMessage.{Recalculated, Error, Routed}
import io.pathfinder.websockets.pushing.EventBusActor
import io.pathfinder.websockets.{WebSocketMessage, Events, ModelTypes}
import play.Logger
import play.api.Play
import play.api.libs.json.{JsResultException, JsString, JsArray, Reads, __, JsNumber, JsObject, Writes, JsValue}
import play.api.libs.ws.{WSResponse, WS}
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.functional.syntax._
import scala.language.postfixOps
import scala.concurrent.duration._

import scala.util.{Failure, Success, Try}

object ClusterRouter {

    type Row = Seq[Int]
    type Matrix = Seq[Row]

    protected val ZERO = BigDecimal(0)
    protected implicit val BigDecimalReads = Reads.JsNumberReads.map(_.value)

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

        case class Recalculate(client: ActorRef) extends ClusterRouterMessage
    }

    object DistanceFinder {

        val googleMaps = WS.url("https://maps.googleapis.com/maps/api/distancematrix/json")
        val apiKey = Play.configuration.getString("google.key").orElse{
            Logger.warn("No API key set in application.conf file")
            None
        }

        def makeRequest(origins: TraversableOnce[(Double, Double)], dests: TraversableOnce[(Double, Double)]): Future[WSResponse] = {
            val req = apiKey.fold {
                googleMaps.withQueryString(
                    "origins" -> origins.map(latlng => f"${latlng._1}%.4f,${latlng._2}%.4f").mkString("|"),
                    "destinations" -> dests.map(latlng => f"${latlng._1}%.4f,${latlng._2}%.4f").mkString("|"))
            } { key =>
                googleMaps.withQueryString(
                    "origins" -> origins.map(latlng => f"${latlng._1}%.4f,${latlng._2}%.4f").mkString("|"),
                    "destinations" -> dests.map(latlng => f"${latlng._1}%.4f,${latlng._2}%.4f").mkString("|"),
                    "key" -> key)
            }
            Logger.info(req.toString)
            req.get()
        }

        def parseResponse(res: WSResponse): Try[(Matrix,Matrix)] = Try(
            res.json.validate(
                (__ \ "rows").read[Seq[JsValue]].map(
                    _.map(_.validate(
                        (__ \ "elements").read[Seq[JsValue]].map(
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
            if(origins.isEmpty || dests.isEmpty)
                Future.successful((Seq.empty, Seq.empty))
            else makeRequest(origins, dests).map{ resp =>
                Logger.info(resp.body)
                resp
            }.map(parseResponse).flatMap(
                _.recover{case t => return Future.failed(t)}.map(Future.successful).get
            )
    }

    abstract sealed class CacheState {
        def asFuture: Future[ClusterRoute]
        def events: Seq[ClusterEvent]
        def version: Int // each cache state has a version number which indicate which route is being held or requested
    }

    object CacheState {

        // a route calculation is in progress
        case class Updating(routes: Future[ClusterRoute], events: Seq[ClusterEvent], version: Int) extends CacheState {
            override def asFuture: Future[ClusterRoute] = routes
        }

        // an up to date route is available
        case class UpToDate(routes: ClusterRoute, version: Int) extends CacheState {
            override def asFuture: Future[ClusterRoute] = {
                Logger.info("Using cached routes")
                Future.successful(routes)
            }
            override def events = Seq.empty
        }
    }
}

class ClusterRouter(clusterPath: String) extends EventBusActor with ActorEventBus with LookupClassification {
    import ClusterRouter._

    @volatile
    private var cachedRoutes: CacheState = handleUpdating(Updating(recalculate(), Seq.empty, 0))

    @volatile
    private var publishedVersion: Int = -1 // is only set within synchronized blocks, so we don't need to use AtomicInteger

    override type Event = (ModelId, Routed)
    override type Classifier = ModelId // subscribe by model and by id

    override protected def classify(event: Event): Classifier = event._1

    override protected def publish(event: Event, subscriber: ActorRef): Unit = {
        subscriber ! event._2
    }

    override def subscribe(client: ActorRef, c: Classifier): Boolean = {
        c match {
            case ClusterPath(path) => super.subscribe(client, ClusterPath(clusterPath))
            case modelId => super.subscribe(client, modelId)
        }
        Logger.info("Websocket: "+ client+" subscribed to route updates for: "+c)
        super.subscribe(client, c)
    }

    def vehicleRouted(route: Route): Routed = Routed(
        ModelTypes.Transport,
        Transport.format.writes(route.vehicle),
        Route.writes.writes(route),
        None
    )

    def publish(cr: ClusterRoute, version: Int): Unit = {
        Logger.info("Published Version check: " + version + " > " + publishedVersion)
        if(version > publishedVersion) { // don't send outdated routes
            publish((ModelId.ClusterPath(clusterPath), cr.routed)) // send list of routes to cluster subscribers
            cr.routes.foreach { route =>
                val routeJson: JsValue = Route.writes.writes(route)
                val vehicleJson: JsValue = Transport.format.writes(route.vehicle)

                // publish vehicles to vehicle subscribers
                publish((ModelId.TransportId(route.vehicle.id), Routed(ModelTypes.Transport, vehicleJson, routeJson, None)))
                route.actions.tail.collect {
                    case PickUp(lat, lng, com) =>
                        val comJson = Commodity.format.writes(com) // publish commodities to commodity subscribers
                        publish((ModelId.CommodityId(com.id), Routed(ModelTypes.Commodity, comJson, routeJson, None)))
                    case _Else => Unit
                }
            }
            publishedVersion = version
        }
    }

    private def addErrorHandling[E](f: Future[E]): Future[E] = {
        f.onFailure{
            case e: Throwable =>
                Logger.warn("Error updating routes for cluster: " + clusterPath)
                Logger.trace(e.getMessage, e.getStackTrace)
        }
        f
    }

    private def after[E](f: Future[E]): Future[E] =
        akka.pattern.after(0.seconds, Global.actorSystem.scheduler)(f)

    override protected def mapSize(): Int = 16

    // returns Some(ClusterRoute) when the routes can be changed without a recalculation, otherwise it returns None
    private def handleEvent(e: ClusterEvent, cr: ClusterRoute): Option[ClusterRoute] =
        e match {
            case ClusterEvent(Events.Updated, v: Transport) =>
                Logger.info("vehicle Updated received: " + e)
                Logger.info("for route: " + cr)
                if (TransportStatus.Offline.equals(v.status)) {
                    if (cr.routes.exists(_.vehicle.id == v.id)) {
                        None
                    } else {
                        Some(cr)
                    }
                } else {
                    var found = 0
                    val replacement = cr.routes.map {
                        route =>
                            if (route.vehicle.id == v.id) {
                                found += 1
                                route.copy(vehicle = v, actions = new Action.Start(v) +: route.actions.tail)
                            } else route
                    }
                    if (found == 1) {
                        // good to go
                        Some(cr.copy(routes = replacement))
                    } else {
                        if (found > 1) {
                            Logger.warn(
                                "Received vehicle update for vehicle:" + v.id + " with " + found + " routes in cluster:" + clusterPath + ", routing is probably broken."
                            )
                        }
                        None // vehicle was turned Online
                    }
                }
            case ClusterEvent(event, model) => None
        }

    private def handleEvents(es: Seq[ClusterEvent], init: ClusterRoute): (ClusterRoute,Boolean) =
        (
            es.foldLeft(init) {
                (cr, e) => handleEvent(e, cr).getOrElse(return (cr, false))
            },
            true
        )

    private def handleUpdating(u: Updating): Updating = {
        handleUpdating(u.routes, u.version)
        u
    }

    private def handleUpdating(futureRoutes: Future[ClusterRoute], version: Int): Unit = {
        futureRoutes.onComplete { routeTry =>
            routeTry.map { cr =>
                synchronized {
                    Logger.info("Update received: " + cachedRoutes.version + " == " + version)
                    if(cachedRoutes.version == version) {
                        handleEvents(cachedRoutes.events, cr) match {
                            case (ncr, true) =>
                                cachedRoutes = CacheState.UpToDate(ncr, version)
                                publish(ncr, version)
                            case (ncr, false) =>
                                cachedRoutes = handleUpdating(Updating(recalculate(), Seq.empty, version+1))
                                publish(ncr, version)
                        }
                    } else publish(cr, version)
                }
            }
        }
    }

    /*
     * The cluster router just recalculates whenever a route request or update occurs
     */
    override def receive: Receive = {
        case Recalculate(client) =>
            synchronized {
                Logger.info("ClusterRouter received recalculate request")
                cachedRoutes match {
                    case UpToDate(routes, version) =>
                        val updating = handleUpdating(Updating(recalculate(), Seq.empty, version + 1))
                        updating.routes.onSuccess{
                            case x => client ! Recalculated(Cluster.removeAppFromPath(clusterPath))
                        }
                        updating.routes.onFailure{
                            case e => client ! WebSocketMessage.Error("Failed to recalculate route: " + e.getMessage)
                        }
                    case Updating(future, events, version) => // we ignore the events since we are updating everything
                        val next = after(future).flatMap(x => recalculate())
                        cachedRoutes = handleUpdating(Updating(
                            next,
                            Seq.empty,
                            version + 1
                        ))
                        next.onSuccess{
                            case x => client ! Recalculated(Cluster.removeAppFromPath(clusterPath))
                        }
                        next.onFailure {
                            case e => client ! WebSocketMessage.Error("Failed to recalculate route: " + e.getMessage)
                        }
                }
            }
        case e: ClusterEvent =>
            if(e.model match {
                case v: Transport => v.cluster.id == clusterPath
                case c: Commodity => c.cluster.id == clusterPath
                case cl: Cluster => cl.id == clusterPath
            }) {
                synchronized {
                    Logger.info("Received event :" + e)
                    Logger.info("Cached Routes: " + cachedRoutes)
                    cachedRoutes match {
                        case UpToDate(cr, v) =>
                            cachedRoutes = handleEvent(e, cr).map { ncr =>
                                publish(ncr, v + 1)
                                UpToDate(ncr, v + 1)
                            }.getOrElse {
                                handleUpdating(Updating(recalculate(), Seq.empty, v + 1))
                            }
                        case u: Updating => cachedRoutes = u.copy(events = u.events :+ e)
                    }
                }
            }
        case RouteRequest(client, mId) => cachedRoutes.asFuture.recoverWith{
            case t =>
                client ! Error("Failed to route cluster: "+t.getMessage)
                Future.failed(t)
        }.foreach { rc =>
            mId match {
                case ModelId.ClusterPath(path) => client ! rc.routed
                case ModelId.TransportId(id) => rc.routes.find(_.vehicle.id == id).foreach { route =>
                    client ! vehicleRouted(route).withoutApp
                }
                case ModelId.CommodityId(id) =>
                    var commodity: Commodity = null
                    rc.routes.find { route =>
                        route.actions.exists {
                            case PickUp(lat, lng, com) =>
                                if (com.id == id) {
                                    commodity = com
                                    true
                                } else false
                            case x => false
                        }
                    }.foreach { route =>
                        client ! Routed(
                            ModelTypes.Commodity,
                            Commodity.format.writes(commodity),
                            Route.writes.writes(route),
                            None
                        ).withoutApp
                    }
            }
        }
        case _Else => super.receive(_Else)
    }

    val matrixWriter: Writes[Matrix] = Writes.seq[Row] //Json.writes[Array[Array[Double]]]

    def parseRoutingResponse(vehicles: Seq[Transport], commodities: Seq[Commodity], result: WSResponse): Try[Seq[Route]] =
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

    private def recalculate(): Future[ClusterRoute] = {

        val cluster: Cluster = Cluster.Dao.read(clusterPath).getOrElse{
            Logger.warn("Cluster with id: "+clusterPath+" missing")
            // return Future.failed(null)
            throw new scala.Error("NO CLUSTER WITH ID: " + clusterPath)
        }
        Logger.info("RECALCULATING")
        val vehicles = cluster.transports.filter(v => TransportStatus.Online.equals(v.status))
        val commodities = cluster.commodities.filter(
            c => CommodityStatus.Waiting.equals(c.status) || CommodityStatus.PickedUp.equals(c.status)
        )
        if (vehicles.size <= 0) {
            Logger.info("Someone asked Router to recalculate but there are no vehicles in cluster.")
            return Future.successful(ClusterRoute(clusterPath, Seq.empty, commodities))
        }

        if (commodities.size <= 0) {
            Logger.info("someone asked for Router to recalculate but there are no commodities in cluster")
            return Future.successful(ClusterRoute(clusterPath, vehicles.map(v => Route(v, Seq(new Action.Start(v)))), Seq.empty))
        }

        val starts = vehicles.map(x => (x.latitude, x.longitude))
        val pickups = commodities.map(c => (c.startLatitude, c.startLongitude))
        val dropOffs = commodities.map(c => (c.endLatitude, c.endLongitude))
        val body: Future[JsValue] = for {
            (startToPickUpDist, startToPickUpDur) <- DistanceFinder.find(starts, pickups)
            (startToDropOffDist, startToDropOffDur) <- DistanceFinder.find(starts, dropOffs)
            (pickUpToDropOffDist, pickUpToDropOffDur) <- DistanceFinder.find(pickups, dropOffs)
            (pickUpToPickUpDist, pickUpToPickUpDur) <- DistanceFinder.find(pickups, pickups)
            (dropOffToPickUpDist, dropOffToPickUpDur) <- DistanceFinder.find(dropOffs, pickups)
            (dropOffToDropOffDist, dropOffToDropOffDur) <- DistanceFinder.find(dropOffs, dropOffs)
        } yield {
            def makeMatrix(
                startsToPickUps: Matrix,
                startsToDropOffs: Matrix,
                pickUpsToDropOffs: Matrix,
                pickUpsToPickUps: Matrix,
                dropOffsToPickUps: Matrix,
                dropOffsToDropOffs: Matrix
            ) = JsArray((
              pickUpsToPickUps.zip(pickUpsToDropOffs).map(
                  tup => tup._1 ++ tup._2 ++ Seq.fill(vehicles.size)(0)
              ) ++ dropOffsToPickUps.zip(dropOffsToDropOffs).map(
                  tup => tup._1 ++ tup._2 ++ Seq.fill(vehicles.size)(0)
              ) ++ startsToPickUps.zip(startsToDropOffs).map(
                  tup => tup._1 ++ tup._2 ++ Seq.fill(vehicles.size)(0)
              )).map(row => JsArray(row.map(JsNumber(_))))
            )

            val vehicleTable = JsArray(vehicles.indices.map(num => JsNumber(num + 2 * commodities.size + 1)))
            val comTable = JsObject(commodities.indices.map{ num =>
                (
                    (num + commodities.size + 1).toString,
                    JsNumber(BigDecimal(
                        Option(commodities(num).transport).map { vehicle =>
                            vehicles.zipWithIndex.find(kv => kv._1.id == vehicle.id).get._2 + 2 * commodities.size + 1
                        } getOrElse (num + 1)
                    ))
                )
            })
            val capacities = JsObject(cluster.application.capacityParameters.map { p =>
                p.parameter -> JsObject(
                    commodities.zipWithIndex.foldLeft(Seq.empty[(String, JsValue)]) {
                        case (seq, (com, i)) =>
                            val cap = com.metadata.validate((__ \ p.parameter).read(BigDecimalReads)).getOrElse(ZERO)
                            seq :+ (i + 1).toString -> JsNumber(cap) :+
                                   (i + 1 + commodities.size).toString -> JsNumber(-cap)
                    } ++ vehicles.zipWithIndex.map {
                        case (veh, i) =>
                            (i + 1 + 2 * commodities.size).toString -> JsNumber(
                                veh.metadata.validate(
                                    (__ \ p.parameter).read(BigDecimalReads)
                                ).getOrElse(
                                    BigDecimal(Integer.MAX_VALUE)
                                ) - veh.commodities.map(
                                    _.metadata.validate((__ \ p.parameter).read[JsNumber].map(_.value)).getOrElse(ZERO)
                                ).reduceOption(_+_).getOrElse(ZERO)
                            )
                    }
                )
            })
            Logger.info("Capacities: " + capacities)
            val parameters = JsObject(cluster.application.objectiveParameters.map { p =>
                p.parameter -> JsObject(
                    commodities.zipWithIndex.foldLeft(Seq.empty[(String,JsValue)]) {
                        case (seq, (com, i)) =>
                            val par = com.metadata.validate(
                                (__ \ p.parameter).read[JsNumber]
                            ).getOrElse(JsNumber(0))
                            seq :+ (i + 1).toString -> par :+ (i + 1 + commodities.size).toString -> par
                    } ++ vehicles.zipWithIndex.map {
                        case (veh, i) =>
                            (i + 1 + 2 * commodities.size).toString ->
                                veh.metadata.validate((__ \ p.parameter).read[JsNumber]).getOrElse(JsNumber(Integer.MAX_VALUE))
                    }
                )
            } :+ (
                "request_time" -> JsObject(
                    commodities.zipWithIndex.foldLeft(Seq.empty[(String, JsValue)]) {
                        case (seq, (com, i)) =>
                        val time = JsNumber(com.requestTime.getTime / 1000)
                        seq :+ (i + 1).toString -> time :+ (i + 1 + commodities.size).toString -> time
                } ++ vehicles.indices.map(i => (i + 1 + 2 * commodities.size).toString -> JsNumber(0))
            )))
            Logger.info("Parameters: " + parameters)
            val fun = cluster.application.objectiveFunction
            fun.refresh()
            Logger.info("Objective Function : "+fun.id+" : "+fun.function)
            JsObject(Seq(
                "commodities" -> comTable,
                "vehicles" -> vehicleTable,
                "capacities" -> capacities,
                "distances" -> makeMatrix(
                    startToPickUpDist,
                    startToDropOffDist,
                    pickUpToDropOffDist,
                    pickUpToPickUpDist,
                    dropOffToPickUpDist,
                    dropOffToDropOffDist
                ),
                "durations" -> makeMatrix(
                    startToPickUpDur,
                    startToDropOffDur,
                    pickUpToDropOffDur,
                    pickUpToPickUpDur,
                    dropOffToPickUpDur,
                    dropOffToDropOffDur
                ),
                "parameters" -> parameters,
                "objective" -> JsString(fun.function)
            ))
        }
        addErrorHandling(body.recoverWith[JsValue]{
            case t =>
                Logger.error("Failed to request and receive route data from google maps api", t)
                Future.failed(t)
        }.flatMap[WSResponse]{ json =>
            Logger.info("Sending message: " + json)
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
        }.flatMap { resp =>
            parseRoutingResponse(vehicles, commodities, resp).map(routes =>
                Future.successful(ClusterRoute(clusterPath, routes, Seq.empty))).recover{
                    case t => Future.failed(t)
                }.get
        })
    }
}
