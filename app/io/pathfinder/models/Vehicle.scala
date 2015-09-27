package io.pathfinder.models

import com.avaje.ebean.Model
import javax.persistence.{Id,Entity,GeneratedValue,GenerationType,Column}
import play.api.libs.json.{Format,Reads,Json}
import scala.collection.mutable.Buffer
import io.pathfinder.data.{Update,EbeanCrudDao}

object Vehicle {

    val finder: Model.Finder[Long,Vehicle] = new Model.Finder[Long,Vehicle](classOf[Vehicle])

    object Dao extends EbeanCrudDao[Long,Vehicle,Model.Finder[Long,Vehicle]](finder){
        override def construct = new Vehicle
    }

    implicit val format: Format[Vehicle] = Json.format[Vehicle]

    implicit val updateReads: Reads[VehicleUpdate] = Json.reads[VehicleUpdate]

    case class VehicleUpdate(
        latitude:  Option[Double],
        longitude: Option[Double],
        capacity:  Option[Int]
    ) extends Update[Vehicle] {
        override def apply(v: Vehicle): Boolean = {
            latitude.map  ( v.latitude  = _ )
            longitude.map ( v.longitude = _ )
            capacity.map  ( v.capacity  = _ )
            latitude.isDefined && longitude.isDefined && capacity.isDefined
        }
    }

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
