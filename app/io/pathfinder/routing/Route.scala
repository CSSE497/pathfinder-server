package io.pathfinder.routing

import io.pathfinder.models.Vehicle
import io.pathfinder.routing.Action.Start
import play.api.libs.json.Json

import scala.collection.mutable

case class Route(vehicle: Vehicle, actions: Seq[Action]) {
    def this(v: Vehicle) = this(v,Seq(new Start(v)))
}

object Route {
    private class RouteBuilder(vehicle: Vehicle) extends mutable.Builder[Action,Route] {
        private val actions = Seq.newBuilder[Action] += new Action.Start(vehicle)

        override def +=(elem: Action): RouteBuilder.this.type = {
            actions += elem
            this
        }

        override def result(): Route = Route(vehicle, actions.result())

        override def clear(): Unit = {
            actions.clear()
            actions += new Action.Start(vehicle)
        }
    }
    import Action.writes
    import Vehicle.format
    def newBuilder(vehicle: Vehicle): mutable.Builder[Action,Route] = new RouteBuilder(vehicle)
    val writes = Json.writes[Route]
}