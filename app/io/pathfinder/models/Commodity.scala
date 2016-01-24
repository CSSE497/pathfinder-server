package io.pathfinder.models

import java.util.Date
import javax.persistence.{GenerationType, Column, Id, Entity, GeneratedValue, JoinColumn, ManyToOne}

import com.avaje.ebean.Model

import io.pathfinder.data.Resource
import io.pathfinder.websockets.ModelTypes
import io.pathfinder.websockets.pushing.WebSocketDao

import play.api.libs.json.{JsObject, Writes, Json, Format}

object Commodity {
    val finder: Model.Find[Long,Commodity] = new Model.Finder[Long,Commodity](classOf[Commodity])

    object Dao extends WebSocketDao[Commodity](finder) {

        override def modelType: ModelTypes.Value = ModelTypes.Commodity

        override def writer: Writes[Commodity] = Commodity.format
    }

    implicit val statusFormat = CommodityStatus.format
    implicit val format: Format[Commodity] = Json.format[Commodity]
    implicit val resourceFormat: Format[CommodityResource] = Json.format[CommodityResource]

    case class CommodityResource(
        id: Option[Long],
        startLatitude:  Option[Double],
        endLatitude: Option[Double],
        startLongitude:  Option[Double],
        endLongitude: Option[Double],
        status: Option[CommodityStatus],
        metadata:  Option[JsObject],
        clusterId: Option[String],
        vehicleId: Option[Long]
    ) extends Resource[Commodity] {
        override def update(c: Commodity): Option[Commodity] = {
            startLatitude.foreach(c.startLatitude  = _)
            startLongitude.foreach(c.startLongitude = _)
            endLatitude.foreach(c.endLatitude = _)
            endLongitude.foreach(c.endLongitude = _)
            metadata.foreach(c.metadata  = _)
            status.foreach {
                newStatus =>
                    c.status = newStatus
                    if (c.status.equals(CommodityStatus.PickedUp)) {
                        vehicleId.orElse(return None).foreach { id => c.vehicle = Vehicle.Dao.read(id).getOrElse(return None) }
                    } else {
                        if (CommodityStatus.Waiting.equals(newStatus)) {
                            c.requestTime = new Date
                        }
                        c.vehicle = null // don't know what should happen here
                    }
            }
            Some(c)
        }

        def create(cluster: Cluster): Option[Commodity] = for {
                startLatitude <- startLatitude
                startLongitude <- startLongitude
                endLatitude <- endLatitude
                endLongitude <- endLongitude
            } yield {
                val stat = status.getOrElse(CommodityStatus.Inactive)
                val c = Commodity(
                    0,
                    startLatitude,
                    startLongitude,
                    endLatitude,
                    endLongitude,
                    stat,
                    metadata.getOrElse(JsObject(Seq.empty)),
                    vehicleId,
                    cluster.id
                )
                c.cluster = cluster
                if(CommodityStatus.Waiting.equals(stat)) {
                    c.requestTime = new Date
                }
                c
            }

        override def create: Option[Commodity] = for {
            id <- clusterId
            cluster <- Cluster.Dao.read(id)
            model <- create(cluster)
        } yield model
    }

    def apply(
        id: Long,
        startLatitude:
        Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
        status: CommodityStatus,
        metadata: JsObject,
        vehicleId: Option[Long],
        clusterId: String
    ): Commodity = {
        val c = new Commodity
        c.id = id
        c.startLatitude = startLatitude
        c.startLongitude = startLongitude
        c.endLatitude = endLatitude
        c.endLongitude = endLongitude
        c.status = status
        c.metadata = metadata
        c.vehicle = vehicleId.flatMap{
            vId => Vehicle.Dao.read(vId).orElse(throw new IllegalArgumentException("no vehicle with " + " id: " + vId))
        }.orNull
        c.cluster = Cluster.Dao.read(clusterId).getOrElse(
            throw new IllegalArgumentException("No Cluster With Id: " + clusterId)
        )
        c
    }

    def unapply(c: Commodity): Option[
        (Long, Double, Double, Double, Double, CommodityStatus, JsObject, Option[Long], String)
    ] = Some((
            c.id,
            c.startLatitude,
            c.startLongitude,
            c.endLatitude,
            c.endLongitude,
            c.status,
            c.metadata,
            Option(c.vehicle).map(_.id),
            c.cluster.id
        ))
}

@Entity
class Commodity() extends Model with HasId with HasCluster {

    @Id
    @Column(name="id", nullable=false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name="startLatitude", nullable=false)
    var startLatitude: Double = 0

    @Column(name="startLongitude", nullable=false)
    var startLongitude: Double = 0

    @Column(name="endLatitude", nullable=false)
    var endLatitude: Double = 0

    @Column(name="endLongitude", nullable=false)
    var endLongitude: Double = 0

    @Column(nullable = false)
    var status: CommodityStatus = CommodityStatus.Inactive

    @Column(length = 255)
    var metadata: JsObject = JsObject(Seq.empty)

    @JoinColumn
    @ManyToOne(optional = true)
    var vehicle: Vehicle = null

    @JoinColumn
    @ManyToOne
    var cluster: Cluster = null

    @Column
    var requestTime: Date = new Date(0)
}
