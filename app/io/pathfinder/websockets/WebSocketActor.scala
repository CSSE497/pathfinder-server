package io.pathfinder.websockets

import akka.actor.{Props, Actor, ActorRef}
import io.pathfinder.websockets.ModelTypes._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.pathfinder.websockets.controllers.{CommoditySocketController, ClusterSocketController, WebSocketController, VehicleSocketController}

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

    override def receive = {
        case c: ControllerMessage => controllers.get(c.model).map(
            _.receive(c)
        ).getOrElse(
            Some(Error("No Controller for model: "+c.model))
        ).foreach(client ! _)

        case Subscribe(cluster, model, event, id) => client ! Error("Not Implemented")

        case UnSubscribe(cluster, model, event, id) => client ! Error("Not Implemented")

        case UnknownMessage(value) => client ! Error("Received unknown message: "+value.toString)
    }
}
