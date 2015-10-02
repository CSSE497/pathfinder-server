package io.pathfinder.websockets

import play.api.libs.json.{Format,Json,Reads,Writes,JsObject}
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.json._ // JSON library

/**
 * Contains all of the web socket messages and their json formats
 */
sealed abstract class WebSocketMessage

object WebSocketMessage {
  import ModelTypes.{ModelType,format=>modelFormat}
  import Events.{Event,format=>eventFormat}

  sealed abstract class ControllerMessage extends WebSocketMessage {
    val model: ModelType
  }

  case class ErrorMessage(error: String) extends WebSocketMessage
  implicit val errorMessageFormat = Json.format[ErrorMessage]

  case class UnknownMessage(value: JsValue) extends WebSocketMessage
  implicit object unknownMessageFormat extends Format[UnknownMessage]{
    override def reads(json: JsValue): JsResult[UnknownMessage] = JsSuccess(UnknownMessage(json))
    override def writes(o: UnknownMessage): JsValue = o.value
  }

  case class UnSubscribe(
    cluster: Option[Long],
    model:   Option[ModelType],
    event:   Option[Events.Value],
    id:      Option[Long]
  ) extends WebSocketMessage
  implicit val unSubscribeFormat = Json.format[UnSubscribe]

  case class Subscribe(
    cluster: Long,
    model:   Option[ModelType],
    event:   Option[Event],
    id:      Option[Long]
  ) extends WebSocketMessage
  implicit val subscribeFormat = Json.format[Subscribe]

  case class Create(
    model: ModelType,
    value: JsValue
  ) extends ControllerMessage
  implicit val createFormat = Json.format[Create]

  case class Update(
    model: ModelType,
    id:    Long,
    value: JsValue
  ) extends ControllerMessage
  implicit val updateFormat = Json.format[Update]

  case class Delete(
    model: ModelType,
    id:     Long
  ) extends ControllerMessage
  implicit val deleteFormat = Json.format[Delete]

  case class Read(
    model: ModelType,
    id:   Long
  ) extends ControllerMessage
  implicit val readFormat = Json.format[Read]

  case class Created(
    model: ModelType,
    value: JsValue
  ) extends ControllerMessage
  implicit val createdFormat = Json.format[Created]

  case class Updated(
    model: ModelType,
    value: JsValue
  ) extends ControllerMessage
  implicit val updatedFormat = Json.format[Updated]

  case class Model(
    model: ModelType,
    value: JsValue
  ) extends ControllerMessage
  implicit val modelFormat = Json.format[Model]

  case class Deleted(
    model: ModelType,
    value: JsValue
  ) extends ControllerMessage
  implicit val deletedFormat = Json.format[Deleted]

  case class Subscribed(
    cluster: Long,
    model:   Option[ModelType],
    event:   Option[Events.Value],
    id:      Option[Long]
  ) extends WebSocketMessage
  implicit val subscribedFormat = Json.format[Subscribed]

  case class UnSubscribed(
    cluster: Long,
    model:   Option[ModelType],
    event:   Option[Events.Value],
    id:      Option[Long]
  ) extends WebSocketMessage
  implicit val unSubscribedFormat = Json.format[UnSubscribed]

  implicit val reads: Reads[WebSocketMessage] =
    (JsPath \ "create").read[Create].map(identity[WebSocketMessage]) orElse
    (JsPath \ "read").read[Read].map(identity[WebSocketMessage]) orElse
    (JsPath \ "update").read[Update].map(identity[WebSocketMessage]) orElse
    (JsPath \ "delete").read[Delete].map(identity[WebSocketMessage]) orElse
    (JsPath \ "subscribe").read[Subscribe].map(identity[WebSocketMessage]) orElse
    (JsPath \ "unsubscribe").read[UnSubscribe].map(identity[WebSocketMessage]) orElse
    (JsPath \ "created").read[Created].map(identity[WebSocketMessage]) orElse
    (JsPath \ "model").read[Model].map(identity[WebSocketMessage]) orElse
    (JsPath \ "updated").read[Updated].map(identity[WebSocketMessage]) orElse
    (JsPath \ "deleted").read[Deleted].map(identity[WebSocketMessage]) orElse
    (JsPath \ "subscribed").read[Subscribed].map(identity[WebSocketMessage]) orElse
    (JsPath \ "unsubscribed").read[UnSubscribed].map(identity[WebSocketMessage]) orElse
    errorMessageFormat.map(identity[WebSocketMessage]) orElse unknownMessageFormat.map(identity[WebSocketMessage])

  implicit object writes extends Writes[WebSocketMessage] {

    override def writes(o: WebSocketMessage): JsValue = o match {
      case c: Create       => (JsPath \ "create").write(createFormat).writes(c)
      case r: Read         => (JsPath \ "read").write(readFormat).writes(r)
      case u: Update       => (JsPath \ "update").write(updateFormat).writes(u)
      case d: Delete       => (JsPath \ "delete").write(deleteFormat).writes(d)
      case s: Subscribe    => (JsPath \ "subscribe").write(subscribeFormat).writes(s)
      case u: UnSubscribe  => (JsPath \ "unsubscribe").write(unSubscribeFormat).writes(u)
      case c: Created      => (JsPath \ "created").write(createdFormat).writes(c)
      case m: Model        => (JsPath \ "model").write(modelFormat).writes(m)
      case u: Updated      => (JsPath \ "updated").write(updatedFormat).writes(u)
      case d: Deleted      => (JsPath \ "deleted").write(deletedFormat).writes(d)
      case s: Subscribed   => (JsPath \ "subscribed").write(subscribedFormat).writes(s)
      case u: UnSubscribed => (JsPath \ "unsubscribed").write(unSubscribedFormat).writes(u)
      case e: ErrorMessage => errorMessageFormat.writes(e)
      case u: UnknownMessage => unknownMessageFormat.writes(u)
    }
  }

  implicit val format: Format[WebSocketMessage] = Format(reads,writes)
  implicit val frameFormat: FrameFormatter[WebSocketMessage] = FrameFormatter.jsonFrame[WebSocketMessage]
}