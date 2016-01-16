package io.pathfinder.models

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
        clusterId: Option[String]
    ) extends Resource[Commodity] {
        override def update(c: Commodity): Option[Commodity] = {
            startLatitude.foreach(c.startLatitude  = _)
            startLongitude.foreach(c.startLongitude = _)
            endLatitude.foreach(c.endLatitude = _)
            endLongitude.foreach(c.endLongitude = _)
            metadata.foreach(c.metadata  = _)
            status.foreach(c.status = _)
            Some(c)
        }

        def create(cluster: Cluster): Option[Commodity] = for {
                startLatitude <- startLatitude
                startLongitude <- startLongitude
                endLatitude <- endLatitude
                endLongitude <- endLongitude
            } yield {
                val c = Commodity(
                    0,
                    startLatitude,
                    startLongitude,
                    endLatitude,
                    endLongitude,
                    status.getOrElse(CommodityStatus.Inactive),
                    metadata.getOrElse(JsObject(Seq.empty))
                )
                c.cluster = cluster
                c
            }

        override def create: Option[Commodity] = for {
            id <- clusterId
            cluster <- Cluster.Dao.read(id)
            model <- create(cluster)
        } yield model
    }

    def apply(id: Long, startLatitude: Double, startLongitude: Double, endLatitude: Double,
              endLongitude: Double, status: CommodityStatus, metadata: JsObject): Commodity = {
        val c = new Commodity
        c.id = id
        c.startLatitude = startLatitude
        c.startLongitude = startLongitude
        c.endLatitude = endLatitude
        c.endLongitude = endLongitude
        c.status = status
        c.metadata = metadata
        c
    }

    def unapply(c: Commodity): Option[(Long, Double, Double, Double, Double, CommodityStatus, JsObject)] =
        Some((c.id, c.startLatitude, c.startLongitude, c.endLatitude, c.endLongitude, c.status, c.metadata))
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

    @JoinColumn(name = "cluster_path")
    @ManyToOne
    var cluster: Cluster = null
}
