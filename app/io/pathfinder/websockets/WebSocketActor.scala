package io.pathfinder.websockets

import akka.actor.{Props, Actor, ActorRef}
import io.pathfinder.models.{Vehicle, Commodity}
import io.pathfinder.routing.Router
import io.pathfinder.websockets.ModelTypes.ModelType
import io.pathfinder.websockets.WebSocketMessage.{RouteSubscribed, RouteSubscribe, Unsubscribed, Unsubscribe, UnknownMessage, Error, Subscribe, Subscribed, ControllerMessage}
import io.pathfinder.websockets.pushing.PushSubscriber

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
                                    obs.subscribeByClusterId(id, client)
                                    Subscribed(cluster, model, None)
                            }
                        }.getOrElse(Error("Subscribe requires either a model id or a cluster id"))
                    }.getOrElse(Error ("Can only subscribe to vehicles or commodities"))
                case RouteSubscribe(model, id) =>
                    if(Router.RouteSubscriber.subscribe(client, model, id))
                        client ! RouteSubscribed(model, id)
                    else
                        client ! Error("id: "+id+" not found for model: "+model)
                case Unsubscribe(None, None, None) =>
                    observers.foreach(_._2.unsubscribe(client))
                    client ! Unsubscribed(None,None,None)
                case Unsubscribe(Some(cId), None, None) =>
                    observers.foreach(_._2.unsubscribeByClusterId(cId, client))
                    client ! Unsubscribed(Some(cId),None,None)
                case Unsubscribe(Some(cId), Some(model), None) =>
                    client ! observers.get(model).map {
                        obs =>
                            obs.unsubscribeByClusterId(cId, client)
                            Unsubscribed(Some(cId), Some(model), None)
                    }.getOrElse(Error("Cannot unsubscribe for model: "+model+" which has no support for subscriptions"))
                case Unsubscribe(None, Some(model), Some(id)) =>
                    client ! observers.get(model).map {
                        obs =>
                            obs.unsubscribeById(id, client)
                            Unsubscribed(None, Some(model), Some(id))
                    }.getOrElse(Error("Cannoth unsubscribe for model: "+model+"which has no support for subscriptions"))
                case u: Unsubscribe =>
                    client ! Error("An unsubscribe message must either have a model id and model type, cluster id and model type, a cluster id, or be empty")
                case UnknownMessage(value) => client ! Error("Received unknown message: " + value.toString)
                case x => client ! Error("received unknown value: "+ x.toString)
            }
        }
    }
}
