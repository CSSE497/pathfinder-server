package io.pathfinder.routing

import play.api.libs.json.Json

case class Route(vehicle: Long, actions: Seq[Action])

object Route {
    import Action.format
    val format = Json.format[Route]
}