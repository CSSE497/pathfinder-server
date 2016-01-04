package io.pathfinder.models

import java.nio.charset.StandardCharsets
import java.util
import java.util.UUID
import javax.persistence.{OneToMany, CascadeType, Id, Column, Entity}

import com.avaje.ebean.Model
import io.pathfinder.data.{EbeanCrudDao, Resource}
import play.api.libs.json.{Json, Format}
import scala.collection.JavaConverters.{asScalaBufferConverter, seqAsJavaListConverter}
import scala.collection.Iterator

object Cluster {
    val finder: Model.Find[String, Cluster] = new Model.Finder[String, Cluster](classOf[Cluster])

    def byPrefix(path: String): Seq[Cluster] =
        finder.query().where().startsWith("path", path).findList().asScala

    object Dao extends EbeanCrudDao[String, Cluster](finder)

    implicit val format: Format[Cluster] = Json.format[Cluster]
    implicit val resourceFormat: Format[ClusterResource] = Json.format[ClusterResource]

    case class ClusterResource(
        path: String,
        vehicles: Option[Seq[Vehicle.VehicleResource]],
        commodities: Option[Seq[Commodity.CommodityResource]]
    ) extends Resource[Cluster] {

        override def update(model: Cluster): Option[Cluster] = None // Cluster Updates are not supported

        /** Creates a new model instance from this resource. */
        override def create: Option[Cluster] = {
            val model = new Cluster
            model.path = path // should validate path
            vehicles.foreach(
                _.foreach {
                    vehicleResource => model.vehicleList.add(
                        (for {
                            id <- vehicleResource.id
                            model <- Vehicle.Dao.read(id)
                            update <- vehicleResource.update(model)
                        } yield update).orElse(vehicleResource.create(model)).getOrElse(return None)
                    )
                }
            )
            commodities.foreach(
                _.foreach {
                    commodityResource => model.commodityList.add(
                        (for {
                            id <- commodityResource.id
                            model <- Commodity.Dao.read(id)
                            update <- commodityResource.update(model)
                        } yield update).orElse(commodityResource.create(model)).getOrElse(return None)
                    )
                }
            )
            Some(model)
        }
    }

    def apply(path: String, vehicles: Seq[Vehicle], commodities: Seq[Commodity], subClusters: Seq[Cluster]): Cluster = {
        val c = new Cluster
        c.path = path
        c.vehicleList.addAll(vehicles.asJava)
        c.commodityList.addAll(commodities.asJava)
        c
    }

    def unapply(c: Cluster): Option[(String, Seq[Vehicle], Seq[Commodity], Seq[Cluster])] =
        Some((c.path, c.vehicles, c.commodities, c.subClusters))
}


@Entity
class Cluster() extends Model {
    @Id
    @Column(nullable = false)
    var path: String = null

    @Column
    var authenticationToken: Array[Byte] = "top secret".getBytes(StandardCharsets.UTF_8)

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var vehicleList: util.List[Vehicle] = new util.ArrayList[Vehicle]()

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var commodityList: util.List[Commodity] = new util.ArrayList[Commodity]()

    def vehicles: Seq[Vehicle] = vehicleList.asScala

    def commodities: Seq[Commodity] = commodityList.asScala

    def subClusters: Seq[Cluster] = Cluster.byPrefix(path+"/")

    def parent: Option[Cluster] = Option(Cluster.finder.byId(path.substring(0,path.lastIndexOf("/"))))

    def parents: Iterator[Cluster] =
        Iterator.iterate[Option[Cluster]](
            this.parent
        )(
            _.flatMap(_.parent)
        ).takeWhile(_.isDefined).map(_.get)

    def descendants: Iterator[Cluster] =             // each level            // combine all the levels
        Iterator.iterate(subClusters.iterator)(_.map(_.subClusters).flatten).takeWhile(_.hasNext).flatten

    def application: Application =
        Application.finder.where().eq("cluster_id",
            (Iterator(this) ++ parents).dropWhile(_.parent.isDefined).next().id
        ).findUnique()

    def application: PathfinderApplication =
        PathfinderApplication.finder.byId(UUID.fromString(path.iterator.takeWhile(_ != '/').mkString))

    override def toString = String.format(
        "Cluster(path: %s, vehicles: %s, commodities: %s)",
        path,
        util.Arrays.toString(vehicleList.toArray),
        util.Arrays.toString(commodityList.toArray))
}
