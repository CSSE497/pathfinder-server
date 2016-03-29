package io.pathfinder.routing

import io.pathfinder.models.Transport
import io.pathfinder.routing.Action.Start
import play.api.libs.json.Json

import scala.collection.mutable

case class Route(transport: Transport, actions: Seq[Action]) {
    def this(v: Transport) = this(v,Seq(new Start(v)))
}

object Route {
    private class RouteBuilder(transport: Transport) extends mutable.Builder[Action,Route] {
        private val actions = Seq.newBuilder[Action] += new Action.Start(transport)

        override def +=(elem: Action): RouteBuilder.this.type = {
            actions += elem
            this
        }

        override def result(): Route = Route(transport, actions.result())

        override def clear(): Unit = {
            actions.clear()
            actions += new Action.Start(transport)
        }
    }
    import Action.writes
    import Transport.format
    def newBuilder(transport: Transport): mutable.Builder[Action,Route] = new RouteBuilder(transport)
    val writes = Json.writes[Route]
}
