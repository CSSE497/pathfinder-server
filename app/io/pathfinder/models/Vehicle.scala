package io.pathfinder.models

import com.avaje.ebean.Model
import javax.persistence.{Id,Entity,GeneratedValue,GenerationType,Column,OneToMany}
import play.api.libs.json.{Format,Reads,Json,JsValue}
import scala.collection.mutable.Buffer

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
        override def apply(v: Vehicle): Boolean = {
            latitude.map  ( v.latitude  = _ )
            longitude.map ( v.longitude = _ )
            capacity.map  ( v.capacity  = _ )
            latitude.isDefined && longitude.isDefined && capacity.isDefined
        }
    }

    override def create = new Vehicle

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable=false)
    var latitude: Double = 0

    @Column(nullable=false)
    var longitude: Double = 0

    @Column(nullable=false)
    var capacity: Int = 0
}
