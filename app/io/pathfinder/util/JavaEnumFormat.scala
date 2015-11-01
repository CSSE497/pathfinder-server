package io.pathfinder.util

import java.util

import play.api.libs.json.Reads.StringReads
import play.api.libs.json.{Writes, JsError, JsResult, JsValue, Format}

import scala.util.Try

class JavaEnumFormat[E <: Enum[E]](c: Class[E]) extends Format[E] {

    private val valueOf = c.getMethod("valueOf", classOf[String])

    override def reads(json: JsValue): JsResult[E] = Try(
        StringReads.reads(json).map(valueOf.invoke(null, _).asInstanceOf[E])
    ).getOrElse(JsError("Invalid Enum Value: "+json+" must be one of: "+c.getEnumConstants.mkString(", ")))

    override def writes(o: E): JsValue = Writes.StringWrites.writes(o.name())
}
