package io.pathfinder.websockets

import akka.actor.{Props, Actor, ActorRef}
import io.pathfinder.models.ModelId.{ClusterPath, CommodityId, TransportId}
import io.pathfinder.models.{Transport, Commodity}
import io.pathfinder.routing.Router
import play.api.Play
import io.pathfinder.websockets.ModelTypes.ModelType
import io.pathfinder.websockets.WebSocketMessage._
import io.pathfinder.websockets.pushing.PushSubscriber
import play.Logger
import io.pathfinder.websockets.controllers.{CommoditySocketController, ClusterSocketController, WebSocketController, VehicleSocketController}
import java.util.UUID
import scala.util.Try
import io.pathfinder.authentication.AuthServer
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.{JsSuccess, JsResult, Format, Json, JsValue, __}
import play.api.libs.functional.syntax._
import io.pathfinder.authentication.AuthenticationStatus

object WebSocketActor {
    private val authenticate = Play.current.configuration.getBoolean("Authenticate").getOrElse(false)

    val controllers: Map[ModelType, WebSocketController] = Map(
        ModelTypes.Transport -> VehicleSocketController,
        ModelTypes.Cluster -> ClusterSocketController,
        ModelTypes.Commodity -> CommoditySocketController
    )

    val observers: Map[ModelType, PushSubscriber] = Map(
        ModelTypes.Transport -> Transport.Dao,
        ModelTypes.Commodity -> Commodity.Dao
    )

    def props(out: ActorRef, app: String) = Props(new WebSocketActor(out, app, UUID.randomUUID().toString()))
}

/**
 * An actor that manages a websocket connection. It allows the client to make api calls as well as subscribe
 * to push notifications.
 */
class WebSocketActor (
    client: ActorRef,
    app: String,
    id: String
) extends Actor {
    import WebSocketActor.{controllers, observers, authenticate}

    def receive: Receive = {
        case Authenticate(opt) =>
            val res = AuthServer.connection(app, id, opt.getOrElse(false))
            res.onSuccess{ case x => client ! Authenticated(x); context.become(authenticated) }
            res.onFailure{ case e => Logger.error("Error from connection request", e); client ! Error(e.getMessage) }
        case m: WebSocketMessage => client ! Error("Not Authenticated")
    }

    private val authenticated: Receive = {
        case m: WebSocketMessage => Try{
            Logger.info("Received Socket Message " + m)
            m.withApp(app).getOrElse{
                Logger.info("Could not find app id " + app)
                client ! Error("Unable to parse cluster id")
            } match {
                case Route(id) =>
                    if(!Router.routeRequest(client, id)) {
                        client ! Error("could not get route, could not find "+ id.modelType + " with id: " + id.id)
                    }
                case RouteSubscribe(id) =>
                    if(Router.subscribeToRoute(client, id))
                        client ! RouteSubscribed(id).withoutApp
                    else
                        client ! Error("id: "+id.toString+" not found for model: "+id.modelType.toString)

                case Recalculate(cId) =>
                    Router.recalculate(client, cId)
                case c: ControllerMessage => controllers.get(c.model).flatMap(_.receive(c, app)).foreach(client ! _)

                case Subscribe(None, _model, Some(id)) =>
                    client ! {
                        id match {
                            case TransportId(vId) =>
                                observers(ModelTypes.Transport).subscribeById(vId, client)
                                Subscribed(None, None, Some(id)).withoutApp
                            case CommodityId(cId) =>
                                observers(ModelTypes.Commodity).subscribeById(cId, client)
                                Subscribed(None, None, Some(id)).withoutApp
                            case ClusterPath(path) =>
                                observers.values.foreach(_.subscribeByClusterPath(path, client))
                                Subscribed(Some(path), None, Some(id)).withoutApp
                            case _Else => Error("Only subscriptions to vehicles and commodities are supported")
                        }
                    }

                case Subscribe(Some(path), None, None) =>
                    observers.values.foreach(_.subscribeByClusterPath(path, client))
                    client ! Subscribed(Some(path), None, Some(ClusterPath(path)))

                case Subscribe(Some(path), Some(modelType), None) =>
                    client ! observers.get(modelType).map{ obs =>
                        obs.subscribeByClusterPath(path, client)
                        Subscribed(Some(path), Some(modelType), None).withoutApp
                    }.getOrElse(Error("Subscriptions to clusters by cluster not supported"))

                // unsubscribe from everything
                case Unsubscribe(None, None, None) =>
                    observers.foreach(_._2.unsubscribe(client))
                    client ! Unsubscribed(None,None,None).withoutApp

                // unsubscribe from a specified cluster
                case Unsubscribe(Some(cId), None, None) =>
                    observers.foreach(_._2.unsubscribeByClusterPath(cId, client))
                    client ! Unsubscribed(Some(cId),None,None).withoutApp

                // unsubscribe from cluster for models of a specified type
                case Unsubscribe(Some(cId), Some(model), None) =>
                    client ! observers.get(model).map {
                        obs =>
                            obs.unsubscribeByClusterPath(cId, client)
                            Unsubscribed(Some(cId), Some(model), None).withoutApp
                    }.getOrElse(Error("Cannot unsubscribe for model: "+model+" which has no support for subscriptions"))

                // unsibscribe from a single model
                case Unsubscribe(None, modelType, Some(id)) =>
                    client ! {
                        id match {
                            case TransportId(vId) =>
                                observers(ModelTypes.Transport).unsubscribeById(vId, client)
                                Unsubscribed(None, modelType, Some(id)).withoutApp
                            case CommodityId(cId) =>
                                observers(ModelTypes.Commodity).unsubscribeById(cId, client)
                                Unsubscribed(None, modelType, Some(id)).withoutApp
                            case ClusterPath(path) =>
                                observers.foreach(_._2.unsubscribeByClusterPath(path, client))
                                Unsubscribed(Some(path), None, None).withoutApp
                        }
                    }
                case u: Unsubscribe =>
                    client ! Error("An unsubscribe message must either have a model id and model type, cluster id and model type, a cluster id, or be empty")

                case UnknownMessage(value) => client ! Error("Received unknown message: " + value.toString)

                case e: Error => client ! e

                case x => client ! Error("received unhandled message: "+ x.toString)
            }
        }.recover{ case e =>
            e.printStackTrace()
            client ! Error("Unhandled Exception: " + e.getMessage + " : " + e.getStackTrace.mkString("\n\t"))
        }
    }

    // we could check application options here to see if they want authentication, for now we'll use application.conf
    if(authenticate) {
        client ! ConnectionId(id)
    } else {
        context.become(authenticated)
    }

}
