package io.pathfinder.data

import com.avaje.ebean.Model
import play.db.ebean.Transactional
import scala.collection.JavaConversions.asScalaBuffer

class EbeanCrudDao[K,M <: Model](protected val finder: Model.Find[K,M]) extends CrudDao[K,M] {

    final override def create(model: M): M = {
        model.insert()
        model
    }

    final def create(create: Resource[M]): Option[M] = {
        create.create().map {
            model =>
                model.insert()
                model
        }
    }

    @Transactional
    final override def update(id: K, update: Resource[M]): Option[M] = for {
        model   <- Option(finder.byId(id))
        updated <- update.update(model)
    } yield {
        updated.update()
        updated
    }

    final override def update(model: M): Option[M] = {
        model.update()
        Some(model)
    }

    @Transactional
    final override def delete(id: K): Option[M] =
        Option(finder.byId(id)).map{
            model =>
                model.delete()
                model
        }

    final override def read(id: K): Option[M] = Option(finder.byId(id))

    final override def readAll: Seq[M] = finder.all()
}
