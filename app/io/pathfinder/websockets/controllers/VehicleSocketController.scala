package io.pathfinder.websockets.controllers

import io.pathfinder.models.Vehicle
import io.pathfinder.routing.Action.{DropOff, PickUp}
import io.pathfinder.routing.{Action, Route}
import io.pathfinder.websockets.{WebSocketMessage, ModelTypes}
import io.pathfinder.websockets.WebSocketMessage.{Route => RouteMsg, Routed}

/**
 * manages vehicle API calls
 */
object VehicleSocketController extends WebSocketCrudController[Vehicle](ModelTypes.Vehicle,Vehicle.Dao) {

    override def receive(webSocketMessage: WebSocketMessage) = {
        case RouteMsg(_,id) => Vehicle.Dao.read(id).map{
            v =>
                val actions = v.parent.commodities.foldLeft(Seq.newBuilder[Action]){
                    (builder, com) =>
                        builder += new PickUp(com) += new DropOff(com)
                        builder
                }.result()
                val route = Route(id,actions)
                Routed(ModelTypes.Vehicle,id,Route.format.writes(route))
        } getOrElse WebSocketMessage.Error("No Vehicle with id: "+id)
        case x => super.receive(x)
    }
}

