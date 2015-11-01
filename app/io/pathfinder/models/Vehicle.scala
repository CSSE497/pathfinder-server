package io.pathfinder.models

import com.avaje.ebean.Model
import io.pathfinder.data.{ClusterQueries, Resource}
import io.pathfinder.websockets.ModelTypes
import io.pathfinder.websockets.pushing.WebSocketDao
import javax.persistence.{Enumerated, JoinColumn, ManyToOne, Id, Column, Entity, GeneratedValue, GenerationType}
import play.api.libs.json.{Writes, Format, Json}

object Vehicle {

    val finder: Model.Find[Long,Vehicle] = new Model.Finder[Long,Vehicle](classOf[Vehicle])

    object Dao extends WebSocketDao[Vehicle](finder) with ClusterQueries[Long, Vehicle] {
        override def readByCluster(c: Cluster): Seq[Vehicle] = {
            c.refresh()
            c.vehicles
        }

        override def modelType: ModelTypes.Value = ModelTypes.Vehicle

        override def writer: Writes[Vehicle] = Vehicle.format
    }

    implicit val statusFormat: Format[VehicleStatus] = VehicleStatus.format
    implicit val format: Format[Vehicle] = Json.format[Vehicle]

    implicit val resourceFormat: Format[VehicleResource] = Json.format[VehicleResource]

    case class VehicleResource(
        id:        Option[Long],
        latitude:  Option[Double],
        longitude: Option[Double],
        clusterId: Option[Long],
        status:    Option[VehicleStatus],
        capacity:  Option[Int]
    ) extends Resource[Vehicle] {
        override def update(v: Vehicle): Option[Vehicle] = {
            latitude.foreach(v.latitude = _)
            longitude.foreach(v.longitude = _)
            capacity.foreach(v.capacity = _)
            status.foreach(v.status = _)
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
                val stat = status.getOrElse(VehicleStatus.Offline)
                val v = Vehicle(id.getOrElse(0),lat,lng,stat,cap)
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

    def apply(id: Long, latitude: Double, longitude: Double, status: VehicleStatus, capacity: Int): Vehicle = {
        val v = new Vehicle
        v.id = id
        v.latitude = latitude
        v.longitude = longitude
        v.capacity = capacity
        v.status = status
        v
    }

    def unapply(v: Vehicle): Option[(Long, Double, Double, VehicleStatus, Int)] =
        Some((v.id, v.latitude, v.longitude, v.status, v.capacity))
}

@Entity
class Vehicle() extends Model with HasId with HasCluster {

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
    @JoinColumn
    var cluster: Cluster = null

    @Column(nullable=false)
    @Enumerated
    var status: VehicleStatus = VehicleStatus.Offline

    override def toString = {
        "Vehicle(" + id + ", " + latitude + ", " + longitude + ", " + capacity + ", " + status + ")"
    }
}
