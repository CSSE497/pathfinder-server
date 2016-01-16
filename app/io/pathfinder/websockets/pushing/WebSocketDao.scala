package io.pathfinder.websockets.pushing

import akka.actor.ActorRef
import com.avaje.ebean.Model.Find
import io.pathfinder.config.Global
import io.pathfinder.data.{CrudDao, EbeanCrudDao, ObserverDao}
import io.pathfinder.models.ModelId.ClusterPath
import io.pathfinder.models.{HasId, HasCluster}
import io.pathfinder.routing.Router
import io.pathfinder.websockets.WebSocketMessage.{Updated, Deleted, Created}
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage.{UnsubscribeAll, Unsubscribe, Subscribe, Publish}
import io.pathfinder.websockets.{Events, ModelTypes}
import play.Logger
import play.api.libs.json.Writes

/**
 * this class listens for changed to models so that it can push the changes to registered websocket clients
 */

abstract class WebSocketDao[V <: HasCluster with HasId](dao: CrudDao[Long,V]) extends ObserverDao(dao) with PushSubscriber {

    def this(find: Find[Long, V]) = this(new EbeanCrudDao[Long, V](find))

    def modelType: ModelTypes.Value

    def writer: Writes[V]

    val byIdPusher: ActorRef = Global.actorSystem.actorOf(SocketMessagePusher.props[Long])

    val byClusterPusher: ActorRef = Global.actorSystem.actorOf(SocketMessagePusher.props[String])

    protected def onCreated(model: V): Unit = {
        Logger.info("Adding model to create channel: " + model)
        val msg = Created(modelType, writer.writes(model))
        val id = model.id
        val clusterId = model.cluster.path
        byIdPusher      ! Publish((id, msg))
        byClusterPusher ! Publish((clusterId, msg))
        Router.publish(Events.Created, model)
    }

    protected def onDeleted(model: V): Unit = {
        Logger.info("Adding model to create channel: " + model)
        val msg = Deleted(modelType, writer.writes(model))
        val id = model.id
        val clusterId = model.cluster.path
        byIdPusher      ! Publish((id, msg))
        byClusterPusher ! Publish((clusterId, msg))
        Router.publish(Events.Deleted, model)
    }

    protected def onUpdated(model: V): Unit = {
        Logger.info("Adding model to create channel: "+model)
        val msg = Updated(modelType, writer.writes(model))
        val id = model.id
        val clusterId = model.cluster.path
        byIdPusher      ! Publish((id, msg))
        byClusterPusher ! Publish((clusterId, msg))
        Router.publish(Events.Updated, model)
    }

    override def subscribeByClusterPath(clusterId: String, client: ActorRef): Unit = {
        byClusterPusher ! Subscribe(client, clusterId)
    }

    def subscribeById(id: Long, client: ActorRef): Unit = {
        byIdPusher ! Subscribe(client, id)
    }

    def unsubscribeById(id: Long, client: ActorRef): Unit = {
        byIdPusher ! Unsubscribe(client, id)
    }

    override def unsubscribeByClusterPath(clusterId: String, client: ActorRef): Unit = {
        byClusterPusher ! Unsubscribe(client, clusterId)
    }

    def unsubscribe(client: ActorRef): Unit = {
        byIdPusher ! UnsubscribeAll(client)
        byClusterPusher ! UnsubscribeAll(client)
    }
}
