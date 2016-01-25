package io.pathfinder.websockets

import io.pathfinder.models.{Cluster, ModelId}
import io.pathfinder.websockets.WebSocketMessage.MessageCompanion
import play.Logger
import play.api.libs.json.{JsSuccess, JsResult, Format, Json, JsValue, __}
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.functional.syntax._
import scala.language.postfixOps

/**
 * Contains all of the web socket messages and their json formats
 */
sealed abstract class WebSocketMessage {
    protected type M >: this.type <: WebSocketMessage

    def companion: MessageCompanion[M]

    def message: String = companion.message

    def withApp(app: String): Option[M] = Some(this.asInstanceOf[M])

    def withoutApp: M = this.asInstanceOf[M]

    def toJson: JsValue = companion.format.writes(this.asInstanceOf[M])
}

object WebSocketMessage {
    import ModelTypes.{ModelType, format => modelFormat}
    import Events.{Event, format => eventFormat}

    private val builder = Map.newBuilder[String, MessageCompanion[_ <: WebSocketMessage]]

    sealed abstract class MessageCompanion[M <: WebSocketMessage] {
        def message: String
        def format: Format[M]
    }

    private def addComp(comp: MessageCompanion[_ <: WebSocketMessage]) = builder += comp.message -> comp

    /**
     * These messages are routed to controllers based on the model they contain
     */
    sealed abstract class ControllerMessage extends WebSocketMessage {
        override type M >: this.type <: ControllerMessage
        def model: ModelType
    }

    sealed abstract class ModelMessage extends ControllerMessage {
        override type M >: this.type <: ModelMessage
        def id: ModelId
        override def model = id.modelType
        override def withApp(app: String): Option[M] = id.withAppId(app).map(withId)
        override def withoutApp: M = withId(id.withoutAppId)
        protected def withId(id: ModelId): M
    }

    sealed abstract class SubscriptionMessage extends WebSocketMessage {
        override type M >: this.type <: SubscriptionMessage
        def clusterId: Option[String]
        def model: Option[ModelType]
        def id: Option[ModelId]
        override def withApp(app: String): Option[M] = Some(
            withClusterAndId(
                clusterId.map(Cluster.addAppToPath(_, app).getOrElse(return None)),
                id.map(_.withAppId(app).getOrElse(return None))
            )
        )
        override def withoutApp: M = withClusterAndId(
            clusterId.map(Cluster.removeAppFromPath),
            id.map(_.withoutAppId)
        )
        protected def withClusterAndId(clusterId: Option[String], id: Option[ModelId]): M
    }

    private def simpleModelMessageFormat[M <: ModelMessage](makeMessage: ModelId => M): Format[M] = {
        (__ \ "model").format(ModelTypes.format) and
        (__ \ "id").format[JsValue]
    }.apply[M](
        { (model: ModelType, id: JsValue) => makeMessage(ModelId.read(model, id).get) },
        { mf: M => (mf.id.modelType, ModelId.write(mf.id)) }
    )

    private def subscriptionMessageFormat[M <: SubscriptionMessage](
        makeMessage: (Option[String], Option[ModelType], Option[ModelId]) => M
    ) = {
            (__ \ "clusterId").formatNullable[String] and
            (__ \ "model").formatNullable(ModelTypes.format) and
            (__ \ "id").formatNullable[JsValue]
    }.apply[M](
        { (path: Option[String], model: Option[ModelType], id: Option[JsValue]) =>
            makeMessage(
                path,
                model,
                id.map(i => ModelId.read(model.getOrElse(ModelTypes.Cluster), i).get)
            )
        }, {
            sub: M => (sub.clusterId, sub.model, sub.id.map(ModelId.write))
        }
    )

    /**
     * Standard error messages sent to client that make poor request
     */
    case class Error(error: String) extends WebSocketMessage {
        override type M = Error
        override def companion = Error
    }

    object Error extends MessageCompanion[Error] {
        override val message = "Error"
        override val format = Json.format[Error]
    }
    addComp(Error)

    case class UnknownMessage(value: JsValue) extends WebSocketMessage {
        override type M = UnknownMessage
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
        clusterId: Option[String],
        model: Option[ModelType],
        id: Option[ModelId]
    ) extends SubscriptionMessage {
        override type M = Unsubscribe
        override def companion = Unsubscribe
        override def withClusterAndId(clusterId: Option[String], id: Option[ModelId]) =
            copy(clusterId = clusterId, id = id)
    }

    object Unsubscribe extends MessageCompanion[Unsubscribe] {
        override val message = "Unsubscribe"
        override val format = subscriptionMessageFormat(Unsubscribe.apply)
    }
    addComp(Unsubscribe)

    /**
     * Sent by the client to subscribe to push notifications
     */
    case class Subscribe(
        clusterId: Option[String],
        model: Option[ModelType],
        id: Option[ModelId]
    ) extends SubscriptionMessage {
        override type M = Subscribe
        override def companion = Subscribe
        override def withClusterAndId(clusterId: Option[String], id: Option[ModelId]): M =
            copy(clusterId = clusterId, id = id)
    }

    object Subscribe extends MessageCompanion[Subscribe] {
        override val message = "Subscribe"
        override val format = subscriptionMessageFormat(Subscribe.apply)
    }
    addComp(Subscribe)

    /**
     * Sent by the client to subscribe to route updates
     */
    case class RouteSubscribe(
        id: ModelId
    ) extends ModelMessage {
        override type M = RouteSubscribe
        override def companion = RouteSubscribe
        override def withId(id: ModelId): RouteSubscribe = copy(id = id)
    }

    object RouteSubscribe extends MessageCompanion[RouteSubscribe] {
        override val message = "RouteSubscribe"
        override val format = simpleModelMessageFormat(RouteSubscribe.apply)
    }
    addComp(RouteSubscribe)

    /**
     * Sent by the client to unsubscribe from route updates
     */
    case class RouteUnsubscribe(
        id: ModelId
    ) extends ModelMessage {
        override type M = RouteUnsubscribe
        override def companion = RouteUnsubscribe
        override def withId(id: ModelId): RouteUnsubscribe = copy(id = id)
    }

    object RouteUnsubscribe extends MessageCompanion[RouteUnsubscribe] {
        override val message = "RouteUnsubscribe"
        override val format = simpleModelMessageFormat(RouteUnsubscribe.apply)
    }
    addComp(RouteUnsubscribe)

    case class RouteSubscribed(
        id: ModelId
    ) extends ModelMessage {
        override type M = RouteSubscribed
        override def companion = RouteSubscribed
        override def withId(id: ModelId) = copy(id = id)
    }

    object RouteSubscribed extends MessageCompanion[RouteSubscribed] {
        override val message = "RouteSubscribed"
        override val format = simpleModelMessageFormat(RouteSubscribed.apply)
    }
    addComp(RouteSubscribed)

    /**
     * Sent by the client to create a new model
     */
    case class Create(
        model: ModelType,
        value: JsValue
    ) extends ControllerMessage {
        override type M = Create
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
        id: ModelId,
        value: JsValue
    ) extends ModelMessage {
        override type M = Update
        override def companion = Update
        override def withId(id: ModelId) = copy(id = id)
    }

    object Update extends MessageCompanion[Update] {
        override val message = "Update"
        override val format = {
            (__ \ "model").format(ModelTypes.format) and
            (__ \ "id").format[JsValue] and
            (__ \ "value").format[JsValue]
        }.apply[Update](
            { (model: ModelType, id: JsValue, value: JsValue) => Update(ModelId.read(model, id).get, value) },
            { u: Update => (u.id.modelType, ModelId.write(u.id), u.value) }
        )
    }
    addComp(Update)


    /**
     * Sent by the client to delete the specified model
     */
    case class Delete(
        id: ModelId
    ) extends ModelMessage {
        override type M = Delete
        override def companion = Delete
        override def withId(id: ModelId) = copy(id = id)
    }

    object Delete extends MessageCompanion[Delete] {
        override val message = "Delete"
        override val format = simpleModelMessageFormat(Delete.apply)
    }
    addComp(Delete)

    /**
     * Request for when the client wants a route for a vehicle or commodity
     */
    case class Route(
        id: ModelId
    ) extends ModelMessage {
        override type M = Route
        override def companion = Route
        override def withId(id: ModelId) = copy(id = id)
    }

    object Route extends MessageCompanion[Route] {
        override val message = "Route"
        override val format = simpleModelMessageFormat(Route.apply)
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
        override type M = Routed
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
        id: ModelId
    ) extends ModelMessage {
        override type M = Read
        override def companion = Read
        override protected def withId(id: ModelId): Read = copy(id = id)
    }

    object Read extends MessageCompanion[Read] {
        override val message = "Read"
        override val format = simpleModelMessageFormat(Read.apply)
    }
    addComp(Read)

    /**
     * Sent by the client that wants to get the clusters for an application
     */
    case class GetApplicationCluster(
        id: String
    ) extends WebSocketMessage {
        override type M = GetApplicationCluster
        override def companion = GetApplicationCluster
        override def withApp(app: String): Option[GetApplicationCluster] =
            Cluster.addAppToPath(id, app).map(path => copy(id = path))
        override def withoutApp: GetApplicationCluster =
            copy(id = Cluster.removeAppFromPath(id))
    }

    object GetApplicationCluster extends MessageCompanion[GetApplicationCluster] {
        override val message = "GetApplicationCluster"
        override val format = Json.format[GetApplicationCluster]
    }
    addComp(GetApplicationCluster)

    case class ApplicationCluster(
        id: String,
        value: JsValue
    ) extends WebSocketMessage {
        override type M = ApplicationCluster
        override def companion = ApplicationCluster
        override def withApp(app: String): Option[ApplicationCluster] =
            Cluster.addAppToPath(id, app).map(path => copy(id = path))
        override def withoutApp: ApplicationCluster =
            copy(id = Cluster.removeAppFromPath(id))
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
        override type M = Created
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
        override type M = Updated
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
        model: ModelType,
        value: JsValue
    ) extends ControllerMessage {
        override type M = Model
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
        model: ModelType,
        value: JsValue
    ) extends ControllerMessage {
        override type M = Deleted
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
        clusterId: Option[String],
        model: Option[ModelType],
        id: Option[ModelId]
    ) extends SubscriptionMessage {
        override type M = Subscribed
        override def companion = Subscribed
        override def withClusterAndId(clusterId: Option[String], id: Option[ModelId]): Subscribed =
            copy(clusterId = clusterId, id = id)
    }

    object Subscribed extends MessageCompanion[Subscribed] {
        override val message = "Subscribed"
        override val format = subscriptionMessageFormat(Subscribed.apply)
    }
    addComp(Subscribed)

    /**
     * Message sent to a client that requested to unsubscribe
     */
    case class Unsubscribed(
        clusterId: Option[String],
        model: Option[ModelType],
        id: Option[ModelId]
    ) extends SubscriptionMessage {
        override type M = Unsubscribed
        override def companion = Unsubscribed
        override def withClusterAndId(clusterId: Option[String], id: Option[ModelId]): Unsubscribed =
            copy(clusterId = clusterId, id = id)
    }

    object Unsubscribed extends MessageCompanion[Unsubscribed] {
        override val message = "Unsubscribed"
        override val format = subscriptionMessageFormat(Unsubscribed.apply)
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
