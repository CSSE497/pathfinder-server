package io.pathfinder.models

import javax.persistence._

import com.avaje.ebean.Model

import io.pathfinder.data.{ClusterQueries, Resource}
import io.pathfinder.websockets.ModelTypes
import io.pathfinder.websockets.pushing.WebSocketDao

import play.api.libs.json.{Writes, Json, Format}

object Commodity {
    val finder: Model.Find[Long,Commodity] = new Model.Finder[Long,Commodity](classOf[Commodity])

    object Dao extends WebSocketDao[Commodity](finder) with ClusterQueries[Long, Commodity] {
        override def readByCluster(c: Cluster): Seq[Commodity] = {
            c.refresh()
            c.commodities
        }

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
        param:  Option[Int],
        clusterId: Option[Long]
    ) extends Resource[Commodity] {
        override def update(c: Commodity): Option[Commodity] = {
            startLatitude.foreach(c.startLatitude  = _)
            startLongitude.foreach(c.startLongitude = _)
            endLatitude.foreach(c.endLatitude = _)
            endLongitude.foreach(c.endLongitude = _)
            param.foreach(c.param  = _)
            status.foreach(c.status = _)
            Some(c)
        }

        def create(cluster: Cluster): Option[Commodity] = for {
                startLatitude <- startLatitude
                startLongitude <- startLongitude
                endLatitude <- endLatitude
                endLongitude <- endLongitude
                param <- param
            } yield {
                val c = Commodity(
                    0,
                    startLatitude,
                    startLongitude,
                    endLatitude,
                    endLongitude,
                    status.getOrElse(CommodityStatus.Inactive),
                    param
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
              endLongitude: Double, status: CommodityStatus, param: Int): Commodity = {
        val c = new Commodity
        c.id = id
        c.startLatitude = startLatitude
        c.startLongitude = startLongitude
        c.endLatitude = endLatitude
        c.endLongitude = endLongitude
        c.status = status
        c.param = param
        c
    }

    def unapply(c: Commodity): Option[(Long, Double, Double, Double, Double, CommodityStatus, Int)] =
        Some((c.id, c.startLatitude, c.startLongitude, c.endLatitude, c.endLongitude, c.status, c.param))
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

    @Column(name = "param")
    var param: Int = 0

    @JoinColumn
    @ManyToOne
    var cluster: Cluster = null
}
