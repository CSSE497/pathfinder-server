package io.pathfinder.websockets

import java.util.UUID
import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter

/**
 * Contains all of the web socket messages and their json formats
 */
sealed abstract class WebSocketMessage

object WebSocketMessage {
  import ModelTypes.{ModelType,format=>modelFormat}
  import Events.{Event,format=>eventFormat}

  /**
   * These messages are routed to controllers based on the model they contain
   */
  sealed abstract class ControllerMessage extends WebSocketMessage {
    def model: ModelType
  }

  /**
   * Standard error messages sent to client that make poor request
   */
  case class Error(error: String) extends WebSocketMessage
  implicit val errorFormat = Json.format[Error]

  case class UnknownMessage(value: JsValue) extends WebSocketMessage
  implicit object unknownMessageFormat extends Format[UnknownMessage]{
    override def reads(json: JsValue): JsResult[UnknownMessage] = JsSuccess(UnknownMessage(json))
    override def writes(o: UnknownMessage): JsValue = o.value
  }

  /**
   * Sent by the client to unsubscribe from push notifications
   */
  case class Unsubscribe(
    clusterId: Option[Long],
    model:     Option[ModelType],
    id:        Option[Long]
  ) extends WebSocketMessage
  implicit val unSubscribeFormat = Json.format[Unsubscribe]

  /**
   * Sent by the client to subscribe to push notifications
   */
  case class Subscribe(
    clusterId: Option[Long],
    model:     ModelType,
    id:        Option[Long]
  ) extends WebSocketMessage
  implicit val subscribeFormat = Json.format[Subscribe]

  /**
   * Sent by the client to subscribe to route updates
   */
  case class RouteSubscribe(
    model:     ModelType,
    id:        Long
  ) extends WebSocketMessage
  implicit val routeSubscribeFormat = Json.format[RouteSubscribe]

  /**
   * Sent by the client to unsubscribe from route updates
   */
  case class RouteUnsubscribe(
    model:     ModelType,
    id:        Long
  ) extends WebSocketMessage
  implicit val routeUnsubscribeFormat = Json.format[RouteUnsubscribe]

  case class RouteSubscribed(
    model:     ModelType,
    id:        Long
  ) extends WebSocketMessage
  implicit val routeSubscribedFormat = Json.format[RouteSubscribed]

  /**
   * Sent by the client to create a new model
   */
  case class Create(
    model: ModelType,
    value: JsValue
  ) extends ControllerMessage
  implicit val createFormat = Json.format[Create]

  /**
   * Sent by the client to update a model with the specified id
   */
  case class Update(
    model:  ModelType,
    id:     Long,
    value: JsValue
  ) extends ControllerMessage
  implicit val updateFormat = Json.format[Update]

  /**
   * Sent by the client to delete the specified model
   */
  case class Delete(
    model: ModelType,
    id:    Long
  ) extends ControllerMessage
  implicit val deleteFormat = Json.format[Delete]

  /**
   * Request for when the client wants a route for a vehicle or commodity
   */
  case class Route(
    model: ModelType,
    id:    Long
  ) extends ControllerMessage
  implicit val routeFormat = Json.format[Route]

  /**
   * Response for a route request
   */
  case class Routed(
    model: ModelType,
    value: JsValue,
    route: JsValue
  ) extends ControllerMessage
  implicit val routedFormat = Json.format[Routed]

  /**
   * Sent by the client that wants to read a model from the database
   */
  case class Read(
    model: ModelType,
    id:    Long
  ) extends ControllerMessage
  implicit val readFormat = Json.format[Read]

  /**
   * Sent by the client that wants to get the clusters for an application
   */
  case class GetApplicationCluster(
    id: UUID
  ) extends WebSocketMessage
  implicit val getApplicationClusterFormat = Json.format[GetApplicationCluster]
  implicit val uuidFormat = Format[UUID](
      Reads.StringReads.map(UUID.fromString),
      Writes(id => Writes.StringWrites.writes(id.toString))
  )

  case class ApplicationCluster(
    id: UUID,
    clusterId: Long
  ) extends WebSocketMessage
  implicit  val applicationClusterFormat = Json.format[ApplicationCluster]
  /**
   * Message sent to the client that requested a read
   */
  case class Created(
    model:  ModelType,
    value:  JsValue
  ) extends ControllerMessage
  implicit val createdFormat = Json.format[Created]

  /**
   * Message sent to a client that requested an update
   * or any clients that have subscribed to updates
   */
  case class Updated(
    model:  ModelType,
    value:  JsValue
  ) extends ControllerMessage
  implicit val updatedFormat = Json.format[Updated]

  /**
   * Message sent to a client that requested a read
   */
  case class Model(
    model:  ModelType,
    value:  JsValue
  ) extends ControllerMessage
  implicit val modelFormat = Json.format[Model]

  /**
   * Message sent to a client that requested a delete
   */
  case class Deleted(
    model:  ModelType,
    value:  JsValue
  ) extends ControllerMessage
  implicit val deletedFormat = Json.format[Deleted]

  /**
   * Message sent to a client that requested a subscribe
   */
  case class Subscribed(
    clusterId: Option[Long],
    model:     ModelType,
    id:        Option[Long]
  ) extends WebSocketMessage
  implicit val subscribedFormat = Json.format[Subscribed]

  /**
   * Message sent to a client that requested to unsubscribe
   */
  case class Unsubscribed(
    cluster: Option[Long],
    model:   Option[ModelType],
    id:      Option[Long]
  ) extends WebSocketMessage
  implicit val unSubscribedFormat = Json.format[Unsubscribed]

  /**
   * Converts json into WebSocketMessages
   */
  implicit val reads: Reads[WebSocketMessage] =
    (JsPath \ "create").read[Create].map(identity[WebSocketMessage]) orElse // identity is used because Reads are not covariant
    (JsPath \ "read").read[Read].map(identity[WebSocketMessage]) orElse
    (JsPath \ "update").read[Update].map(identity[WebSocketMessage]) orElse
    (JsPath \ "delete").read[Delete].map(identity[WebSocketMessage]) orElse
    (JsPath \ "subscribe").read[Subscribe].map(identity[WebSocketMessage]) orElse
    (JsPath \ "routesubscribe").read[RouteSubscribe].map(identity[WebSocketMessage]) orElse
    (JsPath \ "routesubscribed").read[RouteSubscribed].map(identity[WebSocketMessage]) orElse
    (JsPath \ "routeunsubscribe").read[RouteUnsubscribe].map(identity[WebSocketMessage]) orElse
    (JsPath \ "unsubscribe").read[Unsubscribe].map(identity[WebSocketMessage]) orElse
    (JsPath \ "created").read[Created].map(identity[WebSocketMessage]) orElse
    (JsPath \ "model").read[Model].map(identity[WebSocketMessage]) orElse
    (JsPath \ "updated").read[Updated].map(identity[WebSocketMessage]) orElse
    (JsPath \ "deleted").read[Deleted].map(identity[WebSocketMessage]) orElse
    (JsPath \ "subscribed").read[Subscribed].map(identity[WebSocketMessage]) orElse
    (JsPath \ "unsubscribed").read[Unsubscribed].map(identity[WebSocketMessage]) orElse
    (JsPath \ "route").read[Route].map(identity[WebSocketMessage]) orElse
    (JsPath \ "routed").read[Routed].map(identity[WebSocketMessage]) orElse
    (JsPath \ "getApplicationCluster").read[GetApplicationCluster].map(identity[WebSocketMessage]) orElse
    (JsPath \ "applicationCluster").read[ApplicationCluster].map(identity[WebSocketMessage])
    errorFormat.map(identity[WebSocketMessage]) orElse unknownMessageFormat.map(identity[WebSocketMessage])

  /**
   * Converts WebSocketMessages into Json
   */
  implicit object writes extends Writes[WebSocketMessage] {

    override def writes(o: WebSocketMessage): JsValue = o match {
      case c: Create       => (JsPath \ "create").write(createFormat).writes(c)
      case r: Read         => (JsPath \ "read").write(readFormat).writes(r)
      case u: Update       => (JsPath \ "update").write(updateFormat).writes(u)
      case d: Delete       => (JsPath \ "delete").write(deleteFormat).writes(d)
      case s: Subscribe    => (JsPath \ "subscribe").write(subscribeFormat).writes(s)
      case u: Unsubscribe  => (JsPath \ "unsubscribe").write(unSubscribeFormat).writes(u)
      case c: Created      => (JsPath \ "created").write(createdFormat).writes(c)
      case m: Model        => (JsPath \ "model").write(modelFormat).writes(m)
      case u: Updated      => (JsPath \ "updated").write(updatedFormat).writes(u)
      case d: Deleted      => (JsPath \ "deleted").write(deletedFormat).writes(d)
      case s: Subscribed   => (JsPath \ "subscribed").write(subscribedFormat).writes(s)
      case u: Unsubscribed => (JsPath \ "unsubscribed").write(unSubscribedFormat).writes(u)
      case s: RouteSubscribe   => (JsPath \ "routesubscribe").write(routeSubscribeFormat).writes(s)
      case u: RouteUnsubscribe => (JsPath \ "routeunsubscribe").write(routeUnsubscribeFormat).writes(u)
      case s: RouteSubscribed  => (JsPath \ "routesubscribed").write(routeSubscribedFormat).writes(s)
      case r: Route        => (JsPath \ "route").write(routeFormat).writes(r)
      case r: Routed       => (JsPath \ "routed").write(routedFormat).writes(r)
      case c: GetApplicationCluster => (JsPath \ "getApplicationCluster").write(getApplicationClusterFormat).writes(c)
      case c: ApplicationCluster    => (JsPath \ "applicationCluster").write(applicationClusterFormat).writes(c)
      case e: Error        => errorFormat.writes(e)
      case u: UnknownMessage => unknownMessageFormat.writes(u)
    }
  }

  /**
   * reads and writes WebSocketMessages from/to Json
   */
  implicit val format: Format[WebSocketMessage] = Format(reads,writes)

  /**
   * reads and writes WebSocketMessages for the WebSocketActor, uses the format above
   */
  implicit val frameFormat: FrameFormatter[WebSocketMessage] = FrameFormatter.jsonFrame[WebSocketMessage]
}
