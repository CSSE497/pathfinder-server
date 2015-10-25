package io.pathfinder.websockets.controllers

import io.pathfinder.models.{Commodity, Cluster, Vehicle}
import io.pathfinder.routing.Action.{Start, DropOff, PickUp}
import io.pathfinder.routing.{Action, Route}
import io.pathfinder.websockets.{WebSocketMessage, ModelTypes}
import io.pathfinder.websockets.WebSocketMessage.{Route => RouteMsg, Error, Subscribe, Routed}
import scala.collection.JavaConversions.asScalaBuffer
/**
 * manages vehicle API calls
 */
object VehicleSocketController extends WebSocketCrudController[Vehicle](ModelTypes.Vehicle,Vehicle.Dao) {

    override def receive(webSocketMessage: WebSocketMessage): Option[WebSocketMessage] = webSocketMessage match {
        case RouteMsg(t,id) => Vehicle.Dao.read(id).map{
                v => Error("Not implemented")
            }.orElse(Some(WebSocketMessage.Error("No Vehicle with id: "+id)))
        case x: WebSocketMessage => super.receive(x)
    }
}
