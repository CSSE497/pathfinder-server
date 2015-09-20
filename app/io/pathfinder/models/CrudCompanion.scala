package io.pathfinder.models
import com.avaje.ebean.Model
import com.avaje.ebean.Model.Finder
import play.api.libs.json.{Format,Reads}

/**
 * @author hansondg
 */
trait CrudCompanion[K,V <: Model]{
    def finder: Finder[K,V]
    /*implicit def reads: Reads[V]*/
    implicit def format: Format[V]
    final implicit def updateReads: Reads[_ <: Update[V]] = Update.reads

    def create(): V
    def Update: UpdateCompanion[V,_ <: Update[V]]

    trait UpdateCompanion[V <: Model,U <: Update[V]] {
        implicit def reads: Reads[U]
    }
    
    trait Update[M <: Model] {
        def apply(model: M): Unit
    }
}
