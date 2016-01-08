package io.pathfinder.websockets.controllers

import io.pathfinder.data.{CrudDao, Resource}
import io.pathfinder.models.ModelId
import io.pathfinder.models.ModelId.ClusterPath
import io.pathfinder.websockets.ModelTypes.ModelType
import io.pathfinder.websockets.WebSocketMessage
import io.pathfinder.websockets.WebSocketMessage._
import play.api.libs.json.{Reads, Writes}
import play.api.Logger
import com.avaje.ebean

/**
 * Adds basic crud support to any implementing controller
 */
abstract class WebSocketCrudController[K,V <: ebean.Model](
    model: ModelType,
    dao: CrudDao[K,V]
)(implicit
    val reads: Reads[V],
    val writes: Writes[V],
    val resources: Reads[_ <: Resource[V]]
) extends WebSocketController {

    override def receive(webSocketMessage: WebSocketMessage): Option[WebSocketMessage] = {
        Logger.info(s"Received web socket crud request $webSocketMessage")
        webSocketMessage match {
            case Update(id, value) => Some(
                resources.reads(value).map { res =>
                    dao.update(id.id.asInstanceOf[K], res).map(
                        m => Updated(model, writes.writes(m))
                    ) getOrElse Error("Could not update " + model + " with id " + id)
                } getOrElse Error("Could not parse json in "+model+" Update Request: "+value)
            )
            case Create(m, value) => Some(
                resources.reads(value).map(
                    dao.create(_).map(
                        m => Created(model, writes.writes(m))
                    ) getOrElse Error("Could not create "+model+" from Create Request: "+value)
                ) getOrElse Error("Could not parse json in "+model+" Create Request")
            )
            case Delete(id) => Some(
                dao.delete(id.id.asInstanceOf[K]).map(
                    m => Deleted(model,writes.writes(m))
                )  getOrElse Error("Could not delete "+model+" with id "+id)
            )
            case Read(id) => Some(
                dao.read(id.id.asInstanceOf[K]).map(
                    m => Model(model,writes.writes(m))
                ) getOrElse Error("Could not delete "+model+" with id "+id)
            )
            case x: WebSocketMessage => Some(Error("No support for message: " + WebSocketMessage.format.writes(x) + " for model: "+model.toString))
        }
    }
}
