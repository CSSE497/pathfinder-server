package io.pathfinder.websockets

import com.avaje.ebean.Model
import com.avaje.ebean.Model.Find
import io.pathfinder.data.{EbeanCrudDao, ObserverDao, CrudDao}
import io.pathfinder.models.Cluster
import play.Logger
import play.api.libs.iteratee.{Iteratee, Enumerator, Concurrent}
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * this class listens for changed to models so that it can push the changes to registered websocket clients
 */

class WebSocketDao[V <: Model](dao: CrudDao[Long,V]) extends ObserverDao(dao) {

    def this(find: Find[Long,V]) = this(new EbeanCrudDao[Long,V](find))

    val (createdEnumerator, createdChannel) = Concurrent.broadcast[V]
    val (deletedEnumerator, deletedChannel) = Concurrent.broadcast[V]
    val (updatedEnumerator, updatedChannel) = Concurrent.broadcast[V]
    val changedEnumerator = Concurrent.unicast[(Events.Value,V)]{
        channel =>
            createdEnumerator.run(Iteratee.foreach(m => channel.push(Events.Created->m)))
            deletedEnumerator.run(Iteratee.foreach(m => channel.push(Events.Deleted->m)))
            updatedEnumerator.run(Iteratee.foreach(m => channel.push(Events.Updated->m)))
    }

    protected def onCreated(model: V): Unit = {
        Logger.info("Adding model to create channel: "+model)
        createdChannel.push(model)
    }

    protected def onDeleted(model: V): Unit = {
        Logger.info("Adding model to create channel: "+model)
        deletedChannel.push(model)
    }

    protected def onUpdated(model: V): Unit = {
        Logger.info("Adding model to create channel: "+model)
        updatedChannel.push(model)
    }

    final def clusterSubscribe(): Enumerator[(Events.Value,V)] = {
        Logger.info("Someone subscribed to cluster updates")
        Concurrent.unicast {
            channel => createdEnumerator.run(Iteratee.foreach {
                // do something about null parents
                case mod => channel.push(Events.Created, mod)
            })
        }
    }
}
