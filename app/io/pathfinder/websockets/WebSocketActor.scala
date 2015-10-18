package io.pathfinder.websockets

import akka.actor.{Props, Actor, ActorRef}
import io.pathfinder.models.{HasId, HasCluster, Vehicle, Commodity}
import io.pathfinder.websockets.ModelTypes.ModelType
import io.pathfinder.websockets.WebSocketMessage.{UnknownMessage, Error, UnSubscribe, Subscribe, Subscribed, ControllerMessage}
import io.pathfinder.websockets.pushing.{PushSubscriber, WebSocketDao}

import play.Logger
import io.pathfinder.websockets.controllers.{CommoditySocketController, ClusterSocketController, WebSocketController, VehicleSocketController}

object WebSocketActor {
    val controllers: Map[ModelType, WebSocketController] = Map(
        ModelTypes.Vehicle -> VehicleSocketController,
        ModelTypes.Cluster -> ClusterSocketController,
        ModelTypes.Commodity -> CommoditySocketController
    )

    val observers: Map[ModelType, PushSubscriber] = Map(
        ModelTypes.Vehicle -> Vehicle.Dao,
        ModelTypes.Commodity -> Commodity.Dao
    )

    def props(out: ActorRef) = Props(new WebSocketActor(out, controllers, observers))
}

/**
 * An actor that manages a websocket connection. It allows the client to make api calls as well as subscribe
 * to push notifications.
 */
class WebSocketActor (
    client: ActorRef,
    controllers: Map[ModelType, WebSocketController],
    observers: Map[ModelType, PushSubscriber]
) extends Actor {

    override def receive = {
        case m: WebSocketMessage => {
            Logger.info("Received Socket Message " + m)
            m match {
                case c: ControllerMessage => controllers.get(c.model).flatMap(_.receive(c)).foreach(client ! _)
                case Subscribe(cluster, model, id) =>
                    client ! observers.get(model).map {
                        obs =>
                            id.map { id =>
                                obs.subscribeById(id, client)
                                Subscribed(cluster, model, Some(id))
                            }.orElse {
                                cluster.map { id =>
                                    obs.subscribeByCluster(id, client)
                                    Subscribed(cluster, model, None)
                            }
                        }.getOrElse(Error("Subscribe requires either a model id or a cluster id"))
                    }.getOrElse(Error ("Can only subscribe to vehicles or commodities"))
                case UnSubscribe(cluster, model, id) =>
                    client ! Error("Not Implemented")
                case UnknownMessage(value) => client ! Error("Received unknown message: " + value.toString)
                case x => client ! Error("received unknown value: "+ x.toString)
            }
        }
    }
}

