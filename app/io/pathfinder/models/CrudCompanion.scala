package io.pathfinder.models
import com.avaje.ebean.Model
import com.avaje.ebean.Model.Finder
import play.api.libs.json.{Format,Reads}
import io.pathfinder.data.Update

/**
 * Any Model using scala should have a companion that implements this trait in order
 * to use it with a CrudController
 */
trait CrudCompanion[K,V]{

    /**
     * Json format for model
     */
    implicit def format: Format[V]
    
    /**
     * Json reads for update for model
     */
    implicit def updateReads: Reads[_ <: Update[V]]

    /**
     * creates a default model
     */
    def create: V
}
