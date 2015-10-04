package io.pathfinder.websockets

import akka.actor.{Props, Actor, ActorRef}
import io.pathfinder.websockets.ModelTypes._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.pathfinder.websockets.controllers.{CommoditySocketController, ClusterSocketController, WebSocketController, VehicleSocketController}
import play.api.libs.iteratee.{Iteratee, Concurrent}

object WebSocketActor {
    val controllers: Map[ModelType,WebSocketController] = Map(
        ModelTypes.Vehicle -> VehicleSocketController,
        ModelTypes.Cluster -> ClusterSocketController,
        ModelTypes.Commodity -> CommoditySocketController
    )

    val observers: Map[ModelType,WebSocketDao] = Map(

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
    observers: Map[ModelType, WebSocketDao]
) extends Actor {
    import WebSocketMessage._

    val (pushEnumerator, pushChannel) = Concurrent.broadcast[WebSocketMessage] // sends Created messages to registered clients

    pushEnumerator(Iteratee.foreach(client ! _))

    override def receive = {
        case c: ControllerMessage => controllers.get(c.model).flatMap(_.receive(c)).foreach(client ! _)
        case Subscribe(cluster, model, event, id) => {
            model.map{
                mType =>
                    val evt = event.getOrElse(Events.Changed)
                    val obs = observers.getOrElse(mType, throw new Error("NO VALUE FOR TYPE "+mType.toString+" IN OBSERVER MAP!"))
                    val en  = obs.eventEnumerators.getOrElse(evt, throw new Error("SOMEONE BROKE THE WEBSOCKETDAO CLASS"))
                    en(Iteratee.foreach(client ! _))
            }
            client ! ErrorMessage("Not Implemented")
        }
        case UnSubscribe(cluster, model, event, id) => {
            client ! ErrorMessage("Not Implemented")
        }
        case UnknownMessage(value) => client ! ErrorMessage("Received unknown message: "+value.toString)
    }
}
