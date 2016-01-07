package io.pathfinder.websockets

import java.util.UUID
import io.pathfinder.websockets.WebSocketMessage.MessageCompanion
import play.Logger
import play.api.libs.json.{Writes, Reads, JsSuccess, JsResult, Format, Json, JsValue, __}
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.functional.syntax._
import scala.language.postfixOps

/**
 * Contains all of the web socket messages and their json formats
 */
sealed abstract class WebSocketMessage {
    def companion: MessageCompanion[_] // I can't compile using this.type here
    def message: String = companion.message

    // this caste is her because scala is dumb (see above) and this is the easiest way around it
    def toJson: JsValue = companion.format.asInstanceOf[Format[this.type]].writes(this)
}

object WebSocketMessage {

    import ModelTypes.{ModelType, format => modelFormat}
    import Events.{Event, format => eventFormat}

    private val builder = Map.newBuilder[String, MessageCompanion[_ <: WebSocketMessage]]

    sealed abstract class MessageCompanion[M <: WebSocketMessage] {
        def message: String

        def format: Format[M]
    }

    implicit val uuidFormat = Format[UUID](
        Reads.StringReads.map(UUID.fromString),
        Writes(id => Writes.StringWrites.writes(id.toString))
    )

    private def addComp(comp: MessageCompanion[_ <: WebSocketMessage]) = builder += comp.message -> comp

    /**
     * These messages are routed to controllers based on the model they contain
     */
    sealed abstract class ControllerMessage extends WebSocketMessage {
        def model: ModelType
    }

    /**
     * Standard error messages sent to client that make poor request
     */
    case class Error(error: String) extends WebSocketMessage {
        override def companion = Error
    }

    object Error extends MessageCompanion[Error] {
        override val message = "Error"
        override val format = Json.format[Error]
    }
    addComp(Error)

    case class UnknownMessage(value: JsValue) extends WebSocketMessage {
        override def companion = UnknownMessage
    }

    object UnknownMessage extends MessageCompanion[UnknownMessage] {
        override val message = "Unknown"
        override val format = new Format[UnknownMessage] {
            override def reads(json: JsValue): JsResult[UnknownMessage] = JsSuccess(UnknownMessage(json))
            override def writes(o: UnknownMessage): JsValue = o.value
        }
    }
    addComp(UnknownMessage)

    /**
     * Sent by the client to unsubscribe from push notifications
     */
    case class Unsubscribe(
        clusterId: Option[Long],
        model: Option[ModelType],
        id: Option[Long]
    ) extends WebSocketMessage {
        override def companion = Unsubscribe
    }

    object Unsubscribe extends MessageCompanion[Unsubscribe] {
        override val message = "Unsubscribe"
        override val format = Json.format[Unsubscribe]
    }
    addComp(Unsubscribe)

    /**
     * Sent by the client to subscribe to push notifications
     */
    case class Subscribe(
        clusterId: Option[Long],
        model:     ModelType,
        id:        Option[Long]
    ) extends WebSocketMessage {
        override def companion = Subscribe
    }

    object Subscribe extends MessageCompanion[Subscribe] {
        override val message = "Subscribe"
        override val format = Json.format[Subscribe]
    }
    addComp(Subscribe)

    /**
     * Sent by the client to subscribe to route updates
     */
    case class RouteSubscribe(
        model:     ModelType,
        id:        Long
    ) extends WebSocketMessage {
        override def companion = RouteSubscribe
    }

    object RouteSubscribe extends MessageCompanion[RouteSubscribe] {
        override val message = "RouteSubscribe"
        override val format = Json.format[RouteSubscribe]
    }
    addComp(RouteSubscribe)

    /**
     * Sent by the client to unsubscribe from route updates
     */
    case class RouteUnsubscribe(
        model:     ModelType,
        id:        Long
    ) extends WebSocketMessage {
        override def companion = RouteUnsubscribe
    }

    object RouteUnsubscribe extends MessageCompanion[RouteUnsubscribe] {
        override val message = "RouteUnsubscribe"
        override val format = Json.format[RouteUnsubscribe]
    }
    addComp(RouteUnsubscribe)

    case class RouteSubscribed(
        model:     ModelType,
        id:        Long
    ) extends WebSocketMessage {
        override def companion = RouteSubscribed
    }

    object RouteSubscribed extends MessageCompanion[RouteSubscribed] {
        override val message = "RouteSubscribed"
        override val format = Json.format[RouteSubscribed]
    }
    addComp(RouteSubscribed)

    /**
     * Sent by the client to create a new model
     */
    case class Create(
        model: ModelType,
        value: JsValue
    ) extends ControllerMessage {
        override def companion = Create
    }

    object Create extends MessageCompanion[Create] {
        override val message = "Create"
        override val format = Json.format[Create]
    }
    addComp(Create)

    /**
     * Sent by the client to update a model with the specified id
     */
    case class Update(
        model:  ModelType,
        id:     Long,
        value: JsValue
    ) extends ControllerMessage {
        override def companion = Update
    }

    object Update extends MessageCompanion[Update] {
        override val message = "Update"
        override val format = Json.format[Update]
    }
    addComp(Update)

    /**
     * Sent by the client to delete the specified model
     */
    case class Delete(
        model: ModelType,
        id:    Long
    ) extends ControllerMessage {
        override def companion = Delete
    }

    object Delete extends MessageCompanion[Delete] {
        override val message = "Delete"
        override val format = Json.format[Delete]
    }
    addComp(Delete)

    /**
     * Request for when the client wants a route for a vehicle or commodity
     */
    case class Route(
        model: ModelType,
        id:    Long
    ) extends ControllerMessage {
        override def companion = Route
    }

    object Route extends MessageCompanion[Route] {
        override val message = "Route"
        override val format = Json.format[Route]
    }
    addComp(Route)

    /**
     * Response for a route request
     */
    case class Routed(
        model: ModelType,
        value: JsValue,
        route: JsValue
    ) extends ControllerMessage {
        override def companion = Routed
    }

    object Routed extends MessageCompanion[Routed] {
        override val message = "Routed"
        override val format = Json.format[Routed]
    }
    addComp(Routed)

    /**
     * Sent by the client that wants to read a model from the database
     */
    case class Read(
        model: ModelType,
        id:    Long
    ) extends ControllerMessage {
        override def companion = Read
    }

    object Read extends MessageCompanion[Read] {
        override val message = "Read"
        override val format = Json.format[Read]
    }
    addComp(Read)

    /**
     * Sent by the client that wants to get the clusters for an application
     */
    case class GetApplicationCluster(
        id: UUID
    ) extends WebSocketMessage {
        override def companion = GetApplicationCluster
    }

    object GetApplicationCluster extends MessageCompanion[GetApplicationCluster] {
        override val message = "GetApplicationCluster"
        override val format = Json.format[GetApplicationCluster]
    }
    addComp(GetApplicationCluster)

    case class ApplicationCluster(
        id: UUID,
        clusterId: Long
    ) extends WebSocketMessage {
        override def companion = ApplicationCluster
    }

    object ApplicationCluster extends MessageCompanion[ApplicationCluster] {
        override val message = "ApplicationCluster"
        override val format = Json.format[ApplicationCluster]
    }
    addComp(ApplicationCluster)

    /**
     * Message sent to the client that requested a create
     */
    case class Created(
        model: ModelType,
        value: JsValue
    ) extends ControllerMessage {
        override def companion = Created
    }

    object Created extends MessageCompanion[Created] {
        override val message = "Created"
        override val format = Json.format[Created]
    }
    addComp(Created)

    /**
     * Message sent to a client that requested an update
     * or any clients that have subscribed to updates
     */
    case class Updated(
        model: ModelType,
        value: JsValue
    ) extends ControllerMessage {
      override def companion = Updated
    }

    object Updated extends MessageCompanion[Updated] {
        override val message = "Updated"
        override val format = Json.format[Updated]
    }
    addComp(Updated)


    /**
     * Message sent to a client that requested a read
     */
    case class Model(
        model:  ModelType,
        value:  JsValue
    ) extends ControllerMessage {
        override def companion = Model
    }

    object Model extends MessageCompanion[Model] {
        override val message = "Model"
        override val format = Json.format[Model]
    }
    addComp(Model)

    /**
     * Message sent to a client that requested a delete
     */
    case class Deleted(
        model:  ModelType,
        value:  JsValue
    ) extends ControllerMessage {
        override def companion = Deleted
    }

    object Deleted extends MessageCompanion[Deleted] {
        override val message = "Deleted"
        override val format = Json.format[Deleted]
    }
    addComp(Deleted)

    /**
     * Message sent to a client that requested a subscribe
     */
    case class Subscribed(
        clusterId: Option[Long],
        model:     ModelType,
        id:        Option[Long]
    ) extends WebSocketMessage {
        override def companion = Subscribed
    }

    object Subscribed extends MessageCompanion[Subscribed] {
        override val message = "Subscribed"
        override val format = Json.format[Subscribed]
    }
    addComp(Subscribed)

    /**
     * Message sent to a client that requested to unsubscribe
     */
    case class Unsubscribed(
        cluster: Option[Long],
        model:   Option[ModelType],
        id:      Option[Long]
    ) extends WebSocketMessage {
        override def companion = Unsubscribed
    }

    object Unsubscribed extends MessageCompanion[Unsubscribed] {
        override val message = "Unsubscribed"
        override val format = Json.format[Unsubscribed]
    }
    addComp(Unsubscribed)

    val stringToMessage: Map[String, _ <: MessageCompanion[_]] = builder.result()

    Logger.info("stringToMessage: [" + stringToMessage.keys.mkString("|")+"]")
    /**
     * reads and writes WebSocketMessages from/to Json
     */
    implicit val format: Format[WebSocketMessage] = (
        (__ \ "message").format[String] and
        __.format[JsValue]
        ) (
            { case (msg,json) => stringToMessage.get(msg).map{  // reads are not covariant so a cast us required
                _.format.reads(json).recoverTotal(
                    errs => Error("Could not parse json: " + json + "\n" + errs.errors.map(err =>
                        err._1+":"+err._2.mkString("\n\t")
                    ).mkString("\n\n"))
                ).asInstanceOf[WebSocketMessage]
            }.getOrElse(UnknownMessage(json)) },
            { msg => (msg.message, msg.toJson) }
        )

    /**
     * reads and writes WebSocketMessages for the WebSocketActor, uses the format above
     */
    implicit val frameFormat: FrameFormatter[WebSocketMessage] = FrameFormatter.jsonFrame[WebSocketMessage]
}
