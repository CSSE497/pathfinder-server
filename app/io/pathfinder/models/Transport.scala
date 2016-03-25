package io.pathfinder.models

import java.util

import com.avaje.ebean.Model
import io.pathfinder.data.Resource
import io.pathfinder.models.Commodity.CommodityResource
import io.pathfinder.websockets.ModelTypes
import io.pathfinder.websockets.pushing.WebSocketDao
import javax.persistence.{CascadeType, OneToMany, Enumerated, JoinColumn, ManyToOne, Id, Column, Entity, GeneratedValue, GenerationType}
import play.api.libs.json.{JsObject, Writes, Format, Json}
import scala.collection.JavaConverters.{asScalaBufferConverter, seqAsJavaListConverter}

object Transport {

    val finder: Model.Find[Long, Transport] = new Model.Finder[Long, Transport](classOf[Transport])

    object Dao extends WebSocketDao[Transport](finder) {

        override def modelType: ModelTypes.Value = ModelTypes.Transport

        override def writer: Writes[Transport] = Transport.format
    }

    implicit val statusFormat: Format[TransportStatus] = TransportStatus.format
    implicit val format: Format[Transport] = Json.format[Transport]

    implicit val resourceFormat: Format[TransportResource] = Json.format[TransportResource]

    case class TransportResource(
        id:        Option[Long],
        latitude:  Option[Double],
        longitude: Option[Double],
        clusterId: Option[String],
        status:    Option[TransportStatus],
        metadata:  Option[JsObject]
    ) extends Resource[Transport] {

        override type R = TransportResource

        override def update(v: Transport): Option[Transport] = {
            latitude.foreach(v.latitude = _)
            longitude.foreach(v.longitude = _)
            status.foreach(v.status = _)
            clusterId.foreach {
                Cluster.Dao.read(_).foreach(v.cluster = _)
            }
            metadata.foreach(v.metadata = _)

            Some(v)
        }

        def create(c: Cluster): Option[Transport] = {
            for {
                lat <- latitude
                lng <- longitude
            } yield {
                val stat = status.getOrElse(TransportStatus.Offline)
                val met = metadata.getOrElse(JsObject(Seq.empty))
                val v = Transport(id.getOrElse(0), lat, lng, stat, met, None, c.id)
                v.cluster = c
                v
            }
        }

        override def create: Option[Transport] = for {
            id <- clusterId
            cluster <- Cluster.Dao.read(id)
            mod <- create(cluster)
        } yield mod

        override def withAppId(appId: String): Option[TransportResource] = Some(
            copy(
                clusterId = clusterId.map(Cluster.addAppToPath(_, appId).getOrElse(return None))
            )
        )

        override def withoutAppId: TransportResource = copy(
            clusterId = clusterId.map(Cluster.removeAppFromPath)
        )
    }

    def apply(
        id: Long,
        latitude: Double,
        longitude: Double,
        status: TransportStatus,
        metadata: JsObject,
        commodities: Option[Seq[Commodity]],
        clusterId: String
    ): Transport = {
        val v = new Transport
        v.id = id
        v.latitude = latitude
        v.longitude = longitude
        v.status = status
        v.metadata = metadata
        commodities.map(cs => v.commodityList.addAll(cs.asJava))
        v.cluster = Cluster.Dao.read(clusterId).getOrElse(
            throw new IllegalArgumentException("No Cluster with id: " + clusterId +" found")
        )
        v
    }

    def unapply(v: Transport): Option[(Long, Double, Double, TransportStatus, JsObject, Option[Seq[Commodity]], String)] =
        Some((
            v.id,
            v.latitude,
            v.longitude,
            v.status,
            v.metadata,
            Some(v.commodities),
            Cluster.removeAppFromPath(v.cluster.id)
        ))
}

@Entity
class Transport() extends Model with HasId with HasCluster {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable=false)
    var latitude: Double = 0

    @Column(nullable=false)
    var longitude: Double = 0

    @ManyToOne
    @JoinColumn
    var cluster: Cluster = null

    @Column(nullable=false)
    @Enumerated
    var status: TransportStatus = TransportStatus.Offline

    @Column(length = 255)
    var metadata: JsObject = JsObject(Seq.empty)

    @OneToMany(mappedBy = "transport", cascade=Array(CascadeType.ALL))
    var commodityList: java.util.List[Commodity] = new util.ArrayList[Commodity]()

    def commodities: Seq[Commodity] = commodityList.asScala

    override def toString = {
        "Transport(" + id + ", " + latitude + ", " + longitude + ", " + status + ", " + metadata +")"
    }
}
