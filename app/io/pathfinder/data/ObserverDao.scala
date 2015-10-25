package io.pathfinder.data

import com.avaje.ebean.Model
import com.avaje.ebean.Model.Find

/**
 * subclasses can use callbacks to listen to changes to models in the specified dao
 */
abstract class ObserverDao[V <: Model](dao: CrudDao[Long,V]) extends CrudDao[Long,V] {

  def this(find: Find[Long,V]) = this(new EbeanCrudDao[Long,V](find))

  protected def onCreated(model: V): Unit
  protected def onDeleted(model: V): Unit
  protected def onUpdated(model: V): Unit

  final override def update(id: Long, update: Resource[V]): Option[V] = for{
    mod <- dao.update(id, update)
    _    = onUpdated(mod)
  } yield mod

  final override def update(model: V): Option[V] = for{
    mod <- dao.update(model)
    _    = onUpdated(mod)
  } yield mod

  final override def delete(id: Long): Option[V] = for{
    mod <- dao.delete(id)
    _    = onDeleted(mod)
  } yield mod

  final override def readAll: Seq[V] = dao.readAll

  final override def read(id: Long): Option[V] = dao.read(id)

  final override def create(model: V): V = {
    val mod: V = dao.create(model)
    onCreated(mod)
    mod
  }

  final override def create(create: Resource[V]): Option[V] = for{
    mod <- dao.create(create)
    _    = onCreated(mod)
  } yield mod
}
