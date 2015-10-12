package io.pathfinder.routing

import io.pathfinder.models.Vehicle
import io.pathfinder.routing.Action.Start
import play.api.libs.json.Json

case class Route(vehicle: Long, actions: Seq[Action]) {
    def this(v: Vehicle) = this(v.id,Seq(new Start(v)))
}

object Route {
    import Action.writes
    val writes = Json.writes[Route]
}