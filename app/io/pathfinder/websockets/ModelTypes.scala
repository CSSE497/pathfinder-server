package io.pathfinder.websockets

import play.api.libs.json._

/**
 * An enum containing the models that a websocket message can use
 */
object ModelTypes extends Enumeration {
    type ModelType = Value
    val Vehicle, Commodity, Cluster = Value

    implicit val format: Format[Value] = Format(
      Json.reads[String].map(ModelTypes.withName),
      { case v: Value => Json.writes[String].writes(v.toString) }
    )
}
