package io.pathfinder.models

import java.util

import com.avaje.ebean.Model
import io.pathfinder.data.Resource
import io.pathfinder.websockets.ModelTypes
import io.pathfinder.websockets.pushing.WebSocketDao
import javax.persistence.{CascadeType, OneToMany, Enumerated, JoinColumn, ManyToOne, Id, Column, Entity, GeneratedValue, GenerationType}
import play.api.libs.json.{JsObject, Writes, Format, Json}
import scala.collection.JavaConverters.{asScalaBufferConverter, seqAsJavaListConverter}

object Vehicle {

    val finder: Model.Find[Long,Vehicle] = new Model.Finder[Long,Vehicle](classOf[Vehicle])

    object Dao extends WebSocketDao[Vehicle](finder) {

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
        clusterId: Option[String],
        status:    Option[VehicleStatus],
        metadata:  Option[JsObject],
        commodities: Option[Seq[Commodity]]
    ) extends Resource[Vehicle] {
        override def update(v: Vehicle): Option[Vehicle] = {
            latitude.foreach(v.latitude = _)
            longitude.foreach(v.longitude = _)
            status.foreach(v.status = _)
            clusterId.foreach {
                Cluster.Dao.read(_).foreach(v.cluster = _)
            }
            metadata.foreach(v.metadata = _)
            commodities.foreach(cs => v.commodityList.addAll(cs.asJava))
            Some(v)
        }

        def create(c: Cluster): Option[Vehicle] = {
            for {
                lat <- latitude
                lng <- longitude
            } yield {
                val stat = status.getOrElse(VehicleStatus.Offline)
                val met = metadata.getOrElse(JsObject(Seq.empty))
                val v = Vehicle(id.getOrElse(0),lat,lng,stat,met,commodities)
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

    def apply(
        id: Long,
        latitude: Double,
        longitude: Double,
        status: VehicleStatus,
        metadata: JsObject,
        commodities: Option[Seq[Commodity]]
    ): Vehicle = {
        val v = new Vehicle
        v.id = id
        v.latitude = latitude
        v.longitude = longitude
        v.status = status
        v.metadata = metadata
        commodities.map(cs => v.commodityList.addAll(cs.asJava))
        v
    }

    def unapply(v: Vehicle): Option[(Long, Double, Double, VehicleStatus, JsObject, Option[Seq[Commodity]])] =
        Some((v.id, v.latitude, v.longitude, v.status, v.metadata, Some(v.commodities)))
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

    @ManyToOne
    @JoinColumn(name = "cluster_path")
    var cluster: Cluster = null

    @Column(nullable=false)
    @Enumerated
    var status: VehicleStatus = VehicleStatus.Offline

    @Column(length = 255)
    var metadata: JsObject = JsObject(Seq.empty)

    @OneToMany(mappedBy = "vehicle", cascade=Array(CascadeType.ALL))
    var commodityList: java.util.List[Commodity] = new util.ArrayList[Commodity]()

    def commodities: Seq[Commodity] = commodityList.asScala

    override def toString = {
        "Vehicle(" + id + ", " + latitude + ", " + longitude + ", " + status + ", " + metadata +")"
    }
}
