package io.pathfinder.websockets.pushing

import akka.actor.ActorRef
import akka.event.{LookupClassification, ActorEventBus}
import com.avaje.ebean.Model
import com.avaje.ebean.Model.Find
import io.pathfinder.config.Global
import io.pathfinder.data.{CrudDao, EbeanCrudDao, ObserverDao}
import io.pathfinder.models.{HasId, HasCluster}
import io.pathfinder.routing.Router
import io.pathfinder.websockets.WebSocketMessage.{Updated, Deleted, Created}
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage.{Subscribe, Publish}
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

    def byIdPusher: ActorRef = Global.actorSystem.actorOf(SocketMessagePusher.props[Long])

    def byClusterPusher: ActorRef = Global.actorSystem.actorOf(SocketMessagePusher.props[Long])

    protected def onCreated(model: V): Unit = {
        Logger.info("Adding model to create channel: " + model)
        val msg = Created(modelType, writer.writes(model))
        val id = model.id
        val clusterId = model.cluster.id
        byIdPusher      ! Publish((id, msg))
        byClusterPusher ! Publish((clusterId, msg))
        Router.ref      ! Publish((clusterId,(modelType, Events.Created, model)))
    }

    protected def onDeleted(model: V): Unit = {
        Logger.info("Adding model to create channel: " + model)
        val msg = Deleted(modelType, writer.writes(model))
        val id = model.id
        val clusterId = model.cluster.id
        byIdPusher      ! Publish((id, msg))
        byClusterPusher ! Publish((clusterId, msg))
        Router.ref      ! Publish((clusterId,(modelType, Events.Created, model)))
    }

    protected def onUpdated(model: V): Unit = {
        Logger.info("Adding model to create channel: "+model)
        val msg = Updated(modelType, writer.writes(model))
        val id = model.id
        val clusterId = model.cluster.id
        byIdPusher      ! Publish((id, msg))
        byClusterPusher ! Publish((clusterId, msg))
        Router.ref      ! Publish((clusterId,(modelType, Events.Created, model)))
    }

    def subscribeByCluster(clusterId: Long, client: ActorRef): Unit = {
        byClusterPusher ! Subscribe(client, clusterId)
    }

    def subscribeById(id: Long, client: ActorRef): Unit = {
        byIdPusher ! Subscribe(client, id)
    }
}
