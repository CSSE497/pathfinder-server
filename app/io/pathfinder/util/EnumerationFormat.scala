package io.pathfinder.util

import play.api.libs.json.{JsError, JsSuccess, Reads, JsString, JsResult, JsValue, Writes, Format}

import scala.util.Try

class EnumerationFormat[E <: Enumeration](enum: E) extends Format[E#Value] {
    override def writes(o: E#Value): JsValue = JsString(o.toString)

    override def reads(json: JsValue): JsResult[E#Value] =
        Reads.StringReads.reads(json).flatMap{
            str => Try(JsSuccess(enum.withName(str))).getOrElse(JsError())
        }
}
