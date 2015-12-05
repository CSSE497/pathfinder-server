package io.pathfinder.models

import java.util
import javax.persistence.{JoinColumn, ManyToOne, OneToMany, CascadeType, Id, GenerationType, Column, GeneratedValue, Entity}

import com.avaje.ebean.Model
import io.pathfinder.data.{ObserverDao, Resource}
import play.api.libs.json.{Json, Format}
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.{mutable, Iterator}

object Cluster {
    val finder: Model.Find[Long, Cluster] = new Model.Finder[Long, Cluster](classOf[Cluster])
    object Dao extends ObserverDao[Cluster](finder) {

        override protected def onCreated(model: Cluster): Unit = {

        }

        override protected def onUpdated(model: Cluster): Unit = {

        }

        override protected def onDeleted(model: Cluster): Unit = {

        }
    }

    val DEFAULT_ID = 0

    implicit val format: Format[Cluster] = Json.format[Cluster]
    implicit val resourceFormat: Format[ClusterResource] = Json.format[ClusterResource]

    case class ClusterResource(
        parentId: Option[Long],
        vehicles: Option[Seq[Vehicle.VehicleResource]],
        commodities: Option[Seq[Commodity.CommodityResource]]
    ) extends Resource[Cluster] {

        override def update(model: Cluster): Option[Cluster] = None // Cluster Updates are not supported

        /** Creates a new model instance from this resource. */
        override def create: Option[Cluster] = {
            val model = new Cluster
            parentId.foreach(
                clusterId =>
                    model.parent = Some(
                        Cluster.Dao.read(clusterId).getOrElse(return None)
                    )
            )
            vehicles.foreach(
                _.foreach {
                    vehicleResource => model.vehicles += (
                        for {
                            id <- vehicleResource.id
                            model <- Vehicle.Dao.read(id)
                            update <- vehicleResource.update(model)
                        } yield update
                    ).orElse(vehicleResource.create(model)).getOrElse(return None)
                }
            )
            commodities.foreach(
                _.foreach {
                    commodityResource => model.commodities += (
                        for {
                            id <- commodityResource.id
                            model <- Commodity.Dao.read(id)
                            update <- commodityResource.update(model)
                      } yield update
                    ).orElse(commodityResource.create(model)).getOrElse(return None)
                }
            )
            Some(model)
        }
    }

    def apply(id: Long, vehicles: Seq[Vehicle], commodities: Seq[Commodity], subClusters: Seq[Cluster]): Cluster = {
        val c = new Cluster
        c.id = id
        c.vehicles ++= vehicles
        c.commodities ++= commodities
        c.subClusters ++= subClusters
        c
    }

    def unapply(c: Cluster): Option[(Long, Seq[Vehicle], Seq[Commodity], Seq[Cluster])] =
        Some((c.id, c.vehicles, c.commodities, c.subClusters))
}

@Entity
class Cluster() extends Model with HasId {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne
    @JoinColumn(name="parent_id", nullable = true)
    var parentCluster: Cluster = null

    @Column
    var authenticationToken: Array[Byte] = "top secret".getBytes

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var vehicleList: util.List[Vehicle] = new util.ArrayList[Vehicle]()

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var commodityList: util.List[Commodity] = new util.ArrayList[Commodity]()

    @OneToMany(mappedBy = "parentCluster", cascade=Array(CascadeType.ALL))
    var clusterList: util.List[Cluster] = new util.ArrayList[Cluster]()

    def vehicles: mutable.Buffer[Vehicle] = vehicleList.asScala

    def commodities: mutable.Buffer[Commodity] = commodityList.asScala

    def subClusters: mutable.Buffer[Cluster] = clusterList.asScala

    def parent: Option[Cluster] = Option(parentCluster)

    def parent_=(opt: Option[Cluster]): Unit = parentCluster = opt.orNull

    def parents: Iterator[Cluster] =
        Iterator.iterate[Option[Cluster]](
            Some(this)
        )(
            _.flatMap(_.parent)
        ).takeWhile(_.isDefined).map(_.get)

    def descendants: Iterator[Cluster] =             // each level            // combine all the levels
        Iterator.iterate(subClusters.iterator)(_.map(_.subClusters).flatten).flatten

    override def toString = String.format(
        "Cluster(id: %s, vehicles: %s, commodities: %s)",
        id.toString,
        util.Arrays.toString(vehicleList.toArray),
        util.Arrays.toString(commodityList.toArray))
}
