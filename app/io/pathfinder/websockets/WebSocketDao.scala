package io.pathfinder.websockets

import javax.management.NotificationBroadcaster

import akka.actor.ActorRef
import com.avaje.ebean.Model.Find
import io.pathfinder.data.{EbeanCrudDao, ObserverDao, CrudDao}
import io.pathfinder.models.HasParent
import io.pathfinder.websockets.ModelTypes.ModelType
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Input, Iteratee, Enumerator, Concurrent}
import play.api.libs.json.Writes
import scala.collection.concurrent
import scala.volatile

abstract class Pusher[V] {
}

class Subscription[V](ch: Channel[V]) extends Channel[V]{

    @volatile
    private var done: Boolean = false

    def isDone = done

    override def end(): Unit = done = true

    override def push(chunk: Input[V]): Unit = ch.push(chunk)

    override def end(e: Throwable): Unit = done = true
}

/**
 * this class listens for changed to models so that it can push the changes to registered websocket clients
 */
class WebSocketDao[V <: HasParent](modelType: ModelType, dao: CrudDao[Long,V])(implicit writes: Writes[V]) extends ObserverDao(dao) {

    def this(modelType: ModelType, find: Find[Long,V]) = this(modelType, new EbeanCrudDao[Long,V](find))

    import WebSocketMessage.{Created => CreatedMsg, Deleted => DeletedMsg, Updated => UpdatedMsg}
    val x =
    val (createdEnumerator, createdChannel) = Concurrent.broadcast[V] // sends Created messages to registered clients
    val (deletedEnumerator, deletedChannel) = Concurrent.broadcast[V] // sends Deleted messages to registered clients
    val (updatedEnumerator, updatedChannel) = Concurrent.broadcast[V] // sends Updated messages to registered clients
    val (changedEnumerator, changedChannel) = Concurrent.broadcast[V]

    createdEnumerator(Iteratee.foreach{
        model =>
            lazy val msg = CreatedMsg(modelType, writes.writes(model))
            clusterSubs.get((model.parent.id,Events.Created)).foreach(_.push(msg))
            modelSubs.get((model.id, Events.Created)).foreach(_.push(msg))
    })
    deletedEnumerator(Iteratee.foreach{
        model =>
            lazy val msg = DeletedMsg(modelType, writes.writes(model))
            clusterSubs.get((model.parent.id,Events.Deleted)).foreach(_.push(msg))
            modelSubs.get((model.id, Events.Deleted)).foreach(_.push(msg))
    })
    updatedEnumerator(Iteratee.foreach{
        model =>
            lazy val msg = UpdatedMsg(modelType, writes.writes(model))
            clusterSubs.get((model.parent.id,Events.Updated)).foreach(_.push(msg))
            modelSubs.get((model.id, Events.Updated)).foreach(_.push(msg))
    })

    /*
     * Cluster -> ModelType -> Event -> Model
     */
    private val clusterChannels: concurrent.Map[(Long,Events.Value),Channel[WebSocketMessage]] = concurrent.TrieMap.empty
    private val modelChannels: concurrent.Map[(Long,Events.Value),Channel[WebSocketMessage]] = concurrent.TrieMap.empty
    private val clusterEnumorators: concurrent.Map[(Long,Events.Value),Channel[WebSocketMessage]] = concurrent.TrieMap.empty
    private val modelEnumerators: concurrent.Map[(Long,Events.Value),Channel[WebSocketMessage]] = concurrent.TrieMap.empty

    protected def onCreated(model: V): Unit = {
        createdChannel.push(model)
    }

    protected def onDeleted(model: V): Unit = {
        deletedChannel.push(model)
    }

    protected def onUpdated(model: V): Unit = {
        updatedChannel.push(model)
    }

    final def clusterSubscribe(client: Channel[WebSocketMessage], cluster: Long, eventOpt: Option[Events.Value]): Subscription[WebSocketMessage] =
        val sub = new Subscription[WebSocketMessage](client)
        eventOpt.getOrElse(Events.Changed) match {
            case Events.Created =>
            case Events.Deleted => new WebSocketSubscription(client,deletedEnumerator)
            case Events.Updated => new WebSocketSubscription(client,updatedEnumerator)
            case Events.Changed => new WebSocketSubscription(client,changedEnumerator)
        }
}
