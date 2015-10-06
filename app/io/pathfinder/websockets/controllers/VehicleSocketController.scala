package io.pathfinder.websockets.controllers

import io.pathfinder.models.{Commodity, Cluster, Vehicle}
import io.pathfinder.routing.Action.{Start, DropOff, PickUp}
import io.pathfinder.routing.{Action, Route}
import io.pathfinder.websockets.{WebSocketMessage, ModelTypes}
import io.pathfinder.websockets.WebSocketMessage.{Route => RouteMsg, Routed}
import scala.collection.JavaConversions.asScalaBuffer
/**
 * manages vehicle API calls
 */
object VehicleSocketController extends WebSocketCrudController[Vehicle](ModelTypes.Vehicle,Vehicle.Dao) {

    override def receive(webSocketMessage: WebSocketMessage): Option[WebSocketMessage] = webSocketMessage match {
        case RouteMsg(t,id) => Vehicle.Dao.read(id).map{
            v =>
                val coms = asScalaBuffer(Commodity.finder.all())
                val actions = coms.foldLeft(Seq.newBuilder[Action] += new Start(v)){
                    (builder, com) =>
                        builder += new PickUp(com) += new DropOff(com)
                        builder
                }.result()
                val route = Route(id,actions)
                Routed(ModelTypes.Vehicle,id,Route.format.writes(route))
            }.orElse(Some(WebSocketMessage.Error("No Vehicle with id: "+id)))
        case x: WebSocketMessage => super.receive(x)
    }
}

