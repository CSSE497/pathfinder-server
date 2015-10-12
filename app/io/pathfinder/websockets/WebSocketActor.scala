package io.pathfinder.websockets

import akka.actor.{Props, Actor, ActorRef}
import io.pathfinder.models.{Vehicle, Commodity}
import io.pathfinder.routing.Router
import io.pathfinder.websockets.ModelTypes.ModelType
import io.pathfinder.websockets.controllers.VehicleSocketController._
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.pathfinder.websockets.controllers.{CommoditySocketController, ClusterSocketController, WebSocketController, VehicleSocketController}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.iteratee.{Iteratee, Concurrent}

object WebSocketActor {
    val controllers: Map[ModelType,WebSocketController] = Map(
        ModelTypes.Vehicle -> VehicleSocketController,
        ModelTypes.Cluster -> ClusterSocketController,
        ModelTypes.Commodity -> CommoditySocketController
    )

    def props(out: ActorRef) = Props(new WebSocketActor(out, controllers))
}

/**
 * An actor that manages a websocket connection. It allows the client to make api calls as well as subscribe
 * to push notifications.
 */
class WebSocketActor (
    client: ActorRef,
    controllers: Map[ModelType, WebSocketController]
) extends Actor {
    import WebSocketMessage._
    Router

    val (pushEnumerator, pushChannel) = Concurrent.broadcast[WebSocketMessage] // sends Created messages to registered clients

    pushEnumerator(Iteratee.foreach(client ! _))

    override def receive = {
        case m: WebSocketMessage =>
            Logger.info("Received Socket Message " + m)
            m match {
                case Create(ModelTypes.Commodity, value) => client ! (
                    Commodity.resourceFormat.reads(value).map(
                        Commodity.Dao.create(_).map(
                            m => {
                                Logger.info("WebSocketActor attempting to subscribe to Commodity: " + m)
                                Router.addCommodity(m)
                                Logger.info("WebSocketActor: Finished adding commodity to router")
                                Created(ModelTypes.Commodity, Commodity.format.writes(m))
                            }
                        ) getOrElse Error("Could not create " + ModelTypes.Commodity + " from Create Request: " + value)
                    ) getOrElse Error("Could not parse json in " + ModelTypes.Commodity + " Create Request")
                )
                case Create(ModelTypes.Vehicle, value) => client ! (
                    Vehicle.resourceFormat.reads(value).map(
                        Vehicle.Dao.create(_).map(
                            m => {
                                Logger.info("WebSocketActor attempting to subscribe to Vehicle: " + m)
                                Router.addVehicle(m, client)
                                Created(ModelTypes.Vehicle, Vehicle.format.writes(m))
                            }
                        ) getOrElse Error("Could not create " + ModelTypes.Vehicle + " from Create Request: " + value)
                    ) getOrElse Error("Could not parse json in " + ModelTypes.Vehicle + " Create Request")
                )
                case c: ControllerMessage => controllers.get(c.model).flatMap(_.receive(c)).foreach(client ! _)
                case Subscribe(cluster, model, event, id) => {
                    client ! Error("Not Implemented")
                }
                case UnSubscribe(cluster, model, event, id) => {
                    client ! Error("Not Implemented")
                }
                case UnknownMessage(value) => client ! Error("Received unknown message: " + value.toString)
                case x => Logger.error("Received unmatchable message: " + m)
            }
    }
}
