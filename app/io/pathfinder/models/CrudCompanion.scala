package io.pathfinder.models
import com.avaje.ebean.Model
import com.avaje.ebean.Model.Finder
import play.api.libs.json.{Format,Reads}

/**
 * Any Model using scala should have a companion that implements this trait in order
 * to use it with a CrudController
 */
trait CrudCompanion[K,V <: Model]{

    /**
     * finder for Ebean model
     */
    def finder: Finder[K,V]

    /**
     * Json format for model
     */
    implicit def format: Format[V]
    
    /**
     * Json reads for update for model
     */
    final implicit def updateReads: Reads[_ <: Update[V]] = Update.reads

    /**
     * creates a default model
     */
    def create(): V

    /**
     * A companion for the Update classes declared in subclasses of CrudCompanion
     */
    def Update: UpdateCompanion[V,_ <: Update[V]]

    /**
     * A trait for UpdateCompanions to inherit
     */
    trait UpdateCompanion[V <: Model,U <: Update[V]] {
        implicit def reads: Reads[U]
    }

    /**
     * An update is a functor that updates an Ebean model(without saving it), it
     * should return true if the update is a full update, where all fields requied
     * for a create are set, otherwise, it returns false.
     */
    trait Update[M <: Model] {
        def apply(model: M): Boolean
    }
}
