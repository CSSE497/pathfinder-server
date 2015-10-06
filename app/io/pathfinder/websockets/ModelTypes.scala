package io.pathfinder.websockets

import play.api.libs.json._

/**
 * An enum containing the models that a websocket message can use
 */
object ModelTypes extends Enumeration {
    type ModelType = Value
    val Vehicle, Commodity, Cluster = Value

    implicit val format: Format[Value] = Format(
      Reads.JsStringReads.map(json => ModelTypes.withName(json.value)),
      Writes(v => JsString(v.toString))
    )
}
