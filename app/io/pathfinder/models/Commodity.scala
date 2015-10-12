package io.pathfinder.models

import javax.persistence.{Column, Entity, GeneratedValue, GenerationType, Id, ManyToOne}

import com.avaje.ebean.Model

import io.pathfinder.websockets.WebSocketDao
import io.pathfinder.data.{ClusterQueries, Resource}
import play.api.libs.json.{Json, Format}

object Commodity {
    val finder: Model.Find[Long,Commodity] = new Model.Finder[Long,Commodity](classOf[Commodity])

    object Dao extends WebSocketDao[Commodity](finder) with ClusterQueries[Long, Commodity] {
        override def readByCluster(c: Cluster): Seq[Commodity] = {
            c.refresh()
            c.commodities
        }
    }

    implicit val format: Format[Commodity] = Json.format[Commodity]
    implicit val resourceFormat: Format[CommodityResource] = Json.format[CommodityResource]

    case class CommodityResource(
        id: Option[Long],
        startLatitude:  Option[Double],
        endLatitude: Option[Double],
        startLongitude:  Option[Double],
        endLongitude: Option[Double],
        param:  Option[Int],
        clusterId: Option[Long]
    ) extends Resource[Commodity] {
        override def update(c: Commodity): Option[Commodity] = {
            startLatitude.foreach(c.startLatitude  = _)
            startLongitude.foreach(c.startLongitude = _)
            endLatitude.foreach(c.endLatitude = _)
            endLongitude.foreach(c.endLongitude = _)
            param.foreach(c.param  = _)
            Some(c)
        }

        def create(cluster: Cluster): Option[Commodity] = for {
                startLatitude <- startLatitude
                startLongitude <- startLongitude
                endLatitude <- endLatitude
                endLongitude <- endLongitude
                param <- param
            } yield {
                val c = Commodity(0, startLatitude, startLongitude, endLatitude, endLongitude, param)
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
              endLongitude: Double, param: Int): Commodity = {
        val c = new Commodity
        c.id = id
        c.startLatitude = startLatitude
        c.startLongitude = startLongitude
        c.endLatitude = endLatitude
        c.endLongitude = endLongitude
        c.param = param
        c
    }

    def unapply(c: Commodity): Option[(Long, Double, Double, Double, Double, Int)] =
        Some((c.id, c.startLatitude, c.startLongitude, c.endLatitude, c.endLongitude, c.param))
}

@Entity
class Commodity() extends Model {
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

    @Column(name = "param")
    var param: Int = 0

    @Column(nullable=false)
    @ManyToOne
    var cluster: Cluster = null
}