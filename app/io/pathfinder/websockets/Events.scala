package io.pathfinder.websockets

import play.api.libs.json._

/**
 * the valid events that a client may subscribe to
 */
object Events extends Enumeration {
  type Event = Value
  val Created, Updated, Deleted = Value

  val reads: Reads[Value] = Reads.JsStringReads.map{str => Events.withName(str.value)}

  object writes extends Writes[Value] {
    override def writes(o: Events.Value): JsValue = JsString(o.toString)
  }

  implicit val format = Format(reads,writes)
}
