package io.pathfinder.models

import java.util
import javax.persistence.{OneToMany, CascadeType, Id, GenerationType, Column, GeneratedValue, Entity}

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
                    vr => model.vehicles += (
                      for {
                          id <- vr.id
                          mod <- Vehicle.Dao.read(id)
                          upd <- vr.update(mod)
                      } yield upd
                      ).orElse(vr.create(model)).getOrElse(return None)
                }
            )
            commodities.foreach(
                _.foreach {
                    cr => model.commodities += (
                        for {
                            id <- cr.id
                            mod <- Commodity.Dao.read(id)
                            upd <- cr.update(mod)
                      } yield upd
                    ).orElse(cr.create(model)).getOrElse(return None)
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

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var vehicleList: util.List[Vehicle] = new util.ArrayList[Vehicle]()

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var commodityList: util.List[Commodity] = new util.ArrayList[Commodity]()

    def vehicles: mutable.Buffer[Vehicle] = vehicleList.asScala
    def commodities: mutable.Buffer[Commodity] = commodityList.asScala
}
