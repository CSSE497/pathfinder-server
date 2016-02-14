package io.pathfinder.routing

import io.pathfinder.models.{Cluster, Commodity}
import io.pathfinder.websockets.ModelTypes
import io.pathfinder.websockets.WebSocketMessage.Routed
import play.api.libs.json.{Writes, JsString, JsObject}

case class ClusterRoute(id: String, routes: Seq[Route], unroutedCommodities: Seq[Commodity]) {
    def routed: Routed = Routed(
        ModelTypes.Cluster,
        JsObject(Seq(
            "id" -> JsString(Cluster.removeAppFromPath(id))
        )),
        Writes.seq(Route.writes).writes(routes),
        Some(unroutedCommodities)
    )
}
