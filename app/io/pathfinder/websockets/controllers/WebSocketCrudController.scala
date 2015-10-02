package io.pathfinder.websockets.controllers

import io.pathfinder.data.{CrudDao, Resource}
import io.pathfinder.websockets.ModelTypes.ModelType
import io.pathfinder.websockets.WebSocketMessage._
import play.api.libs.json.{Reads, Writes}
import com.avaje.ebean

/**
 * Adds basic crud support to any implementing controller
 */
abstract class WebSocketCrudController[V <: ebean.Model](
  model: ModelType,
  dao: CrudDao[Long,V]
)(implicit
  val reads: Reads[V],
  val writes: Writes[V],
  val resources: Reads[_ <: Resource[V]]
) extends WebSocketController {

  override def receive = {
    case Update(m,id,value) => Some(Updated(model, writes.writes(dao.update(id,resources.reads(value).get).get)))
    case Create(m,value)    => Some(Created(model, writes.writes(dao.create(resources.reads(value).get).get)))
    case Delete(m,id)       => Some(Deleted(model, writes.writes(dao.delete(id).get)))
    case Read(m,id)         => Some(Model(model, writes.writes(dao.read(id).get)))
    case x => None
  }
}

