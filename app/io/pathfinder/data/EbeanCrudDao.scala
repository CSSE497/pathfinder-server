package io.pathfinder.data

import com.avaje.ebean.Model
import com.avaje.ebean.Transaction
import play.db.ebean.Transactional
import scala.collection.JavaConversions.asScalaBuffer

class EbeanCrudDao[K,M <: Model,F <: Model.Finder[K,M]](finder: F) extends CrudDao[K,M] {


    final override def create(model: M): M = {
        model.insert()
        model
    }

    @Transactional
    final override def update(id: K, update: Update[M]): Option[M] =
        Option(finder.byId(id)).map{
            model =>
                update(model)
                model.save()
                model
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
