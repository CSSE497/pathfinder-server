package io.pathfinder.websockets

import akka.actor.{Props, Actor, ActorRef}
import io.pathfinder.websockets.ModelTypes._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.pathfinder.websockets.controllers.{WebSocketController, VehicleSocketController}

object WebSocketActor {

  val controllers: Map[ModelType,WebSocketController] = Map(
    ModelTypes.Vehicle -> VehicleSocketController
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

  override def receive = {
    case c: ControllerMessage => controllers.get(c.model).flatMap(_.receive(c)).foreach(client ! _)
    case Subscribe(cluster, model, event, id) => {
      client ! ErrorMessage("Not Implemented")
    }
    case UnSubscribe(cluster, model, event, id) => {
      client ! ErrorMessage("Not Implemented")
    }
    case UnknownMessage(value) => client ! ErrorMessage("Received unknown message: "+value.toString)
  }
}
