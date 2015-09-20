package io.pathfinder.models

import com.avaje.ebean.Model
import javax.persistence.{Id,Entity,GeneratedValue,GenerationType,Transient}
import play.data.validation.Constraints.Required
import play.api.libs.json.{Format,Reads,Json,JsValue}
import javax.inject.Inject

/**
 * @author hansondg
 */

object Vehicle extends CrudCompanion[Long,Vehicle]{
    
    override val format: Format[Vehicle] = Json.format[Vehicle]

    override val finder: Model.Finder[Long,Vehicle] = new Model.Finder[Long,Vehicle](classOf[Vehicle])

    override object Update extends UpdateCompanion[Vehicle,Update]{
        override val reads = Json.reads[Update]
    }

    case class Update(
        latitude:  Option[Double],
        longitude: Option[Double],
        capacity:  Option[Int]
    ) extends super.Update[Vehicle] {
        override def apply(v: Vehicle){
            latitude.map  { v.latitude  = _ }
            longitude.map { v.longitude = _ }
            capacity.map  { v.capacity  = _ }
        }
    }

    override def create() = new Vehicle

    def apply(id: Long, latitude: Double, longitude: Double, capacity: Int): Vehicle = {
        val v = new Vehicle
        v.id = id
        v.latitude = latitude
        v.longitude = longitude
        v.capacity = capacity
        return v
    }

    def unapply(v: Vehicle): Option[(Long, Double, Double, Int)] = Some((v.id, v.latitude, v.longitude, v.capacity))
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