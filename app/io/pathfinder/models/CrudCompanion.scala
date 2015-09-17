package io.pathfinder.models
import com.avaje.ebean.Model
import com.avaje.ebean.Model.Finder
import play.api.libs.json.{Reads,Writes}

/**
 * @author hansondg
 */
abstract trait CrudCompanion[K,V <: Model]{
    def finder: Finder[K,V]
    implicit def reads: Reads[V]
    implicit def writes: Writes[V]
}