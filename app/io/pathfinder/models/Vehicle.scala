package io.pathfinder.models

import com.avaje.ebean.Model
import javax.persistence.{Id,Entity,GeneratedValue,GenerationType,Transient}
import play.data.validation.Constraints.Required
import play.api.libs.json.{Json,Format}
import play.api.libs.functional.syntax._
import javax.inject.Inject

/**
 * @author hansondg
 */

object Vehicle extends CrudCompanion[Long,Vehicle]{
    override val finder: Model.Finder[Long,Vehicle] = new Model.Finder[Long,Vehicle](classOf[Vehicle])
    override val format: Format[Vehicle] = null
    def apply(id: Int, latitude: Double, longitude: Double, capacity: Int): Vehicle = {
        val v = new Vehicle
        v.id = id
        v.latitude = latitude
        v.longitude = longitude
        v.capacity = capacity
        return v
    }
}

@Entity
class Vehicle() extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long = 0

    @Required
    var latitude: Double = 0

    @Required
    var longitude: Double = 0

    @Required
    var capacity: Int = 0
}