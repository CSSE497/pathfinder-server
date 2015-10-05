package io.pathfinder.websockets

import com.avaje.ebean.Model.Find
import io.pathfinder.data.{EbeanCrudDao, ObserverDao, CrudDao}
import io.pathfinder.models.{Cluster, HasParent}
import play.api.libs.iteratee.{Iteratee, Enumerator, Concurrent}

/**
 * this class listens for changed to models so that it can push the changes to registered websocket clients
 */

class WebSocketDao[V <: HasParent](dao: CrudDao[Long,V]) extends ObserverDao(dao) {

    def this(find: Find[Long,V]) = this(new EbeanCrudDao[Long,V](find))

    val (createdEnumerator, createdChannel) = Concurrent.broadcast[V]
    val (deletedEnumerator, deletedChannel) = Concurrent.broadcast[V]
    val (updatedEnumerator, updatedChannel) = Concurrent.broadcast[V]
    val changedEnumerator = Concurrent.unicast[(Events.Value,V)]{
        channel =>
            createdEnumerator(Iteratee.foreach(m => channel.push(Events.Created->m)))
            deletedEnumerator(Iteratee.foreach(m => channel.push(Events.Deleted->m)))
            updatedEnumerator(Iteratee.foreach(m => channel.push(Events.Updated->m)))
    }

    protected def onCreated(model: V): Unit = {
        createdChannel.push(model)
    }

    protected def onDeleted(model: V): Unit = {
        deletedChannel.push(model)
    }

    protected def onUpdated(model: V): Unit = {
        updatedChannel.push(model)
    }

    final def clusterSubscribe(cluster: Cluster) = clusterSubscribe(cluster.id)

    final def clusterSubscribe(cluster: Long): Enumerator[(Events.Value,V)] =
        Concurrent.unicast{
            channel => changedEnumerator(Iteratee.foreach{ // do something about null parents
                case (evt,mod) => if(mod.parent == null || mod.parent.id == cluster) channel.push(evt, mod)
            })
        }

    final def clusterSubscribe(cluster: Cluster, event: Events.Value) = clusterSubscribe(cluster.id, event)

    final def clusterSubscribe(cluster: Long, event: Events.Value): Enumerator[V] =
        Concurrent.unicast {
            channel => (event match {
                case Events.Created => createdEnumerator
                case Events.Deleted => deletedEnumerator
                case Events.Updated => updatedEnumerator
            }).apply(Iteratee.foreach(channel.push))
        }
}
