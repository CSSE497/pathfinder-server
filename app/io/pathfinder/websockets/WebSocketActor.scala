package io.pathfinder.websockets

import akka.actor.{Props, Actor, ActorRef}
import io.pathfinder.models.{Application, Vehicle, Commodity}
import io.pathfinder.models.ModelId.{ClusterPath, CommodityId, VehicleId}
import io.pathfinder.models.{Cluster, ModelId, Application, Vehicle, Commodity}
import io.pathfinder.routing.Router
import io.pathfinder.websockets.ModelTypes.ModelType
import io.pathfinder.websockets.WebSocketMessage._
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

    def props(out: ActorRef) = Props(new WebSocketActor(out))
}

/**
 * An actor that manages a websocket connection. It allows the client to make api calls as well as subscribe
 * to push notifications.
 */
class WebSocketActor (
    client: ActorRef
) extends Actor {
    import WebSocketActor.{controllers, observers}

    override def receive = {
        case m: WebSocketMessage => {
            Logger.info("Received Socket Message " + m)
            m match {
                case RouteSubscribe(id) =>
                    if(Router.subscribeToRoute(client, id))
                        client ! RouteSubscribed(id)
                    else
                        client ! Error("id: "+id.toString+" not found for model: "+id.modelType.toString)

                case c: ControllerMessage => controllers.get(c.model).flatMap(_.receive(c)).foreach(client ! _)
                case GetApplicationCluster(id) => client ! Option(Cluster.finder.byId(id.toString)).map { cluster =>
                    ApplicationCluster(id, Cluster.format.writes(cluster))
                }.getOrElse{
                    Error("No Cluster with path: " + id.toString + " found")
                }
                case Subscribe(None, None, Some(id)) =>
                    client ! {
                        id match {
                            case VehicleId(vId) =>
                                observers(ModelTypes.Vehicle).subscribeById(vId, client)
                                Subscribed(None, None, Some(id))
                            case CommodityId(cId) =>
                                observers(ModelTypes.Commodity).subscribeById(cId, client)
                                Subscribed(None, None, Some(id))
                            case _Else => Error("Only subscriptions to vehicles and commodities are supported")
                        }
                    }

                case Subscribe(Some(path), Some(modelType), None) =>
                    client ! observers.get(modelType).map{ obs =>
                        obs.subscribeByClusterPath(path, client)
                        Subscribed(Some(path), Some(modelType), None)
                    }.getOrElse(Error("Subscriptions to clusters by cluster not supported"))


                // unsubscribe from everything
                case Unsubscribe(None, None, None) =>
                    observers.foreach(_._2.unsubscribe(client))
                    client ! Unsubscribed(None,None,None)


                // unsubscribe from a specified cluster
                case Unsubscribe(Some(cId), None, None) =>
                    observers.foreach(_._2.unsubscribeByClusterPath(cId, client))
                    client ! Unsubscribed(Some(cId),None,None)

                // unsubscribe from cluster for models of a specified type
                case Unsubscribe(Some(cId), Some(model), None) =>
                    client ! observers.get(model).map {
                        obs =>
                            obs.unsubscribeByClusterPath(cId, client)
                            Unsubscribed(Some(cId), Some(model), None)
                    }.getOrElse(Error("Cannot unsubscribe for model: "+model+" which has no support for subscriptions"))

                // unsibscribe from a single model
                case Unsubscribe(None, modelType, Some(id)) =>
                    client ! {
                        id match {
                            case VehicleId(vId) =>
                                observers(ModelTypes.Vehicle).unsubscribeById(vId, client)
                                Unsubscribed(None, modelType, Some(id))
                            case CommodityId(cId) =>
                                observers(ModelTypes.Commodity).unsubscribeById(cId, client)
                                Unsubscribed(None, modelType, Some(id))
                            case ClusterPath(path) =>
                                observers.foreach(_._2.unsubscribeByClusterPath(path, client))
                                Unsubscribed(Some(path), None, None)
                        }
                    }
                case u: Unsubscribe =>
                    client ! Error("An unsubscribe message must either have a model id and model type, cluster id and model type, a cluster id, or be empty")
                case UnknownMessage(value) => client ! Error("Received unknown message: " + value.toString)
                case x => client ! Error("received unknown value: "+ x.toString)
            }
        }
    }
}
