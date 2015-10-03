package io.pathfinder.websockets

import play.api.libs.json._

/**
 * An enum containing the models that a websocket message can use
 */
object ModelTypes extends Enumeration {
  type ModelType = Value
  val Vehicle, Commodity, Cluster = Value
  val reads: Reads[Value] = {
    Reads.JsStringReads.map{str => ModelTypes.withName(str.value)}
  }

  object writes extends Writes[Value] {
    override def writes(o: Value): JsValue = JsString(o.toString)
  }

  implicit val format = Format(reads,writes)
}
