package io.pathfinder.models

import com.avaje.ebean.Model
import javax.persistence._
import io.pathfinder.websockets.{ModelType, WebSocketDao}
import play.api.libs.json.{Format,Json}
import io.pathfinder.data.{Resource,EbeanCrudDao}

object Vehicle {

    val finder: Model.Find[Long,Vehicle] = new Model.Finder[Long,Vehicle](classOf[Vehicle])

    object Dao extends EbeanCrudDao[Long,Vehicle](finder) {
        override def construct = new Vehicle
    }

    implicit val format: Format[Vehicle] = Json.format[Vehicle]

    implicit val resourceFormat: Format[VehicleResource] = Json.format[VehicleResource]

    case class VehicleResource(
        latitude:  Option[Double],
        longitude: Option[Double],
        capacity:  Option[Int]
    ) extends Resource[Vehicle] {
        override def update(v: Vehicle): Option[Vehicle] = {
            latitude.foreach(v.latitude  = _)
            longitude.foreach(v.longitude = _)
            capacity.foreach(v.capacity  = _)
            Some(v)
        }

        override def create(): Option[Vehicle] = for {
            lat <- latitude
            lng <- longitude
            cap <- capacity
        } yield Vehicle(0,lat,lng,cap)
    }

    def apply(id: Long, latitude: Double, longitude: Double, capacity: Int): Vehicle =
        apply(id, latitude, longitude, capacity)

    def unapply(v: Vehicle): Option[(Long, Double, Double, Int)] = Some((v.id, v.latitude, v.longitude, v.capacity))
}

@Entity
class Vehicle() extends Model {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable=false)
    var latitude: Double = 0

    @Column(nullable=false)
    var longitude: Double = 0

    @Column(nullable=false)
    var capacity: Int = 0
}
