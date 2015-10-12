package io.pathfinder.models

import javax.persistence.Column

import com.avaje.ebean.Model
import javax.persistence._
import io.pathfinder.websockets.WebSocketDao
import play.api.libs.json.{Format,Json}
import io.pathfinder.data.{ClusterQueries, Resource}

object Vehicle {

    val finder: Model.Find[Long,Vehicle] = new Model.Finder[Long,Vehicle](classOf[Vehicle])

    object Dao extends WebSocketDao[Vehicle](finder) with ClusterQueries[Long, Vehicle] {
        override def readByCluster(c: Cluster): Seq[Vehicle] = {
            c.refresh()
            c.vehicles
        }
    }

    implicit val format: Format[Vehicle] = Json.format[Vehicle]

    implicit val resourceFormat: Format[VehicleResource] = Json.format[VehicleResource]

    case class VehicleResource(
        id:        Option[Long],
        latitude:  Option[Double],
        longitude: Option[Double],
        clusterId: Option[Long],
        capacity:  Option[Int]
    ) extends Resource[Vehicle] {
        override def update(v: Vehicle): Option[Vehicle] = {
            latitude.foreach(v.latitude  = _)
            longitude.foreach(v.longitude = _)
            capacity.foreach(v.capacity  = _)
            clusterId.foreach {
                Cluster.Dao.read(_).foreach(v.cluster = _)
            }
            Some(v)
        }

        def create(c: Cluster): Option[Vehicle] = {
            for {
                lat <- latitude
                lng <- longitude
                cap <- capacity
            } yield {
                val v = Vehicle(id.getOrElse(0),lat,lng,cap)
                v.cluster = c
                v
            }
        }

        override def create: Option[Vehicle] = for {
            id <- clusterId
            cluster <- Cluster.Dao.read(id)
            mod <- create(cluster)
        } yield mod
    }

    def apply(id: Long, latitude: Double, longitude: Double, capacity: Int): Vehicle = {
        val v = new Vehicle
        v.id = id
        v.latitude = latitude
        v.longitude = longitude
        v.capacity = capacity
        v
    }

    def unapply(v: Vehicle): Option[(Long, Double, Double, Int)] = Some((v.id, v.latitude, v.longitude, v.capacity))
}

@Entity
class Vehicle() extends Model {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable=false)
    var latitude: Double = 0

    @Column(nullable=false)
    var longitude: Double = 0

    @Column(nullable=false)
    var capacity: Int = 0

    @ManyToOne
    @Column(nullable=false)
    var cluster: Cluster = null
}
