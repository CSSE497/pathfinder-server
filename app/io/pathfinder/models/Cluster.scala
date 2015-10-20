package io.pathfinder.models

import java.util
import javax.persistence.{JoinColumn, ManyToOne, OneToMany, CascadeType, Id, GenerationType, Column, GeneratedValue, Entity}

import com.avaje.ebean.Model
import io.pathfinder.data.{Resource, EbeanCrudDao}
import play.api.libs.json.{Json, Format}
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable

object Cluster {
    val finder: Model.Find[Long, Cluster] = new Model.Finder[Long, Cluster](classOf[Cluster])
    object Dao extends EbeanCrudDao[Long, Cluster](finder)

    val DEFAULT_ID = 0

    implicit val format: Format[Cluster] = Json.format[Cluster]
    implicit val resourceFormat: Format[ClusterResource] = Json.format[ClusterResource]

    case class ClusterResource(
        vehicles: Option[Seq[Vehicle.VehicleResource]],
        commodities: Option[Seq[Commodity.CommodityResource]]
    ) extends Resource[Cluster] {

        override def update(model: Cluster): Option[Cluster] = None // Cluster Updates are not supported

        /** Creates a new model instance from this resource. */
        override def create(): Option[Cluster] = {
            val model = new Cluster
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

    def apply(id: Long, vehicles: Seq[Vehicle], commodities: Seq[Commodity]): Cluster = {
        val c = new Cluster
        c.id = id
        c.vehicles ++= vehicles
        c.commodities ++= commodities
        c
    }

    def unapply(c: Cluster): Option[(Long, Seq[Vehicle], Seq[Commodity])] =
        Some((c.id, c.vehicles, c.commodities))
}

@Entity
class Cluster() extends Model {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column
    var authenticationToken: String = "top secret"

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var vehicleList: util.List[Vehicle] = new util.ArrayList[Vehicle]()

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var commodityList: util.List[Commodity] = new util.ArrayList[Commodity]()

    @ManyToOne
    @JoinColumn
    var pathFinderApplication: PathFinderApplication = null

    def vehicles: mutable.Buffer[Vehicle] = vehicleList.asScala
    def commodities: mutable.Buffer[Commodity] = commodityList.asScala

    override def toString = String.format(
        "Cluster(id: %s, vehicles: %s, commodities: %s)",
        id.toString,
        util.Arrays.toString(vehicleList.toArray),
        util.Arrays.toString(commodityList.toArray))
}
