package io.pathfinder.routing

import io.pathfinder.models.Vehicle
import io.pathfinder.routing.Action.Start
import play.api.libs.json.Json

case class Route(vehicle: Vehicle, actions: Seq[Action]) {
    def this(v: Vehicle) = this(v,Seq(new Start(v)))
}

object Route {
    import Action.writes
    import Vehicle.format
    val writes = Json.writes[Route]
}