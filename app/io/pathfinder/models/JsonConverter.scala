package io.pathfinder.models

import com.avaje.ebean.config.ScalarTypeConverter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import play.api.libs.json.JsObject
import play.api.libs.json.Writes.JsonNodeWrites
import play.api.libs.json.Reads.JsonNodeReads

/**
 * This converter allows JsValues to be serialized by Ebean
 */
class JsonConverter extends ScalarTypeConverter[JsObject, JsonNode] {
    override def getNullValue: JsObject = JsObject(Seq.empty)
    override def unwrapValue(jsObj: JsObject): ObjectNode = JsonNodeReads.reads(jsObj).get.asInstanceOf[ObjectNode]
    override def wrapValue(node: JsonNode): JsObject = JsonNodeWrites.writes(node).as[JsObject]
}
