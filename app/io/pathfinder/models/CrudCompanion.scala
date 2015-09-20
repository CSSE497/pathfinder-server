package io.pathfinder.models
import com.avaje.ebean.Model
import com.avaje.ebean.Model.Finder
import play.api.libs.json.Format

/**
 * @author hansondg
 */
trait CrudCompanion[K,V <: Model]{
    def finder: Finder[K,V]
    /*implicit def reads: Reads[V]*/
    implicit def format: Format[V]
}