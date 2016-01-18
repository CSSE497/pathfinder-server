package io.pathfinder.models

import java.util
import javax.persistence.{Transient, OneToMany, CascadeType, Id, Column, Entity}

import com.avaje.ebean.Model
import com.avaje.ebean.annotation.Transactional
import io.pathfinder.data.{EbeanCrudDao, Resource}
import play.api.libs.json.{Json, Format}
import scala.collection.JavaConverters.{asScalaBufferConverter, seqAsJavaListConverter}
import scala.collection.{mutable, Iterator}

object Cluster {
    val finder: Model.Find[String, Cluster] = new Model.Finder[String, Cluster](classOf[Cluster])

    def byPrefix(path: String): Seq[Cluster] =
        finder.query().where().startsWith("id", path).findList().asScala

    object Dao extends EbeanCrudDao[String, Cluster](finder)

    implicit val format: Format[Cluster] = Json.format[Cluster]
    implicit val resourceFormat: Format[ClusterResource] = Json.format[ClusterResource]

    case class ClusterResource(
        id: Option[String],
        name: Option[String],
        vehicles: Option[Seq[Vehicle.VehicleResource]],
        commodities: Option[Seq[Commodity.CommodityResource]],
        subClusters: Option[Seq[ClusterResource]]
    ) extends Resource[Cluster] {

        override def update(model: Cluster): Option[Cluster] = None // Cluster Updates are not supported

        /** Creates a new model instance from this resource. */
        override def create: Option[Cluster] = {
            val model = new Cluster
            model.id = id.getOrElse(return None) // should validate path
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
            subClusters.foreach(
                _.foreach(
                    sub =>
                        sub.copy(id = sub.id.orElse(Some(id + "/" + sub.name.getOrElse(return None))))
                )
            )
            Some(model)
        }
    }

    def apply(id: String, vehicles: Seq[Vehicle], commodities: Seq[Commodity], subClusters: Seq[Cluster]): Cluster = {
        val c = new Cluster
        c.id = id
        c.vehicleList.addAll(vehicles.asJava)
        c.commodityList.addAll(commodities.asJava)
        c.unsavedSubclusters ++= subClusters
        c
    }

    def unapply(c: Cluster): Option[(String, Seq[Vehicle], Seq[Commodity], Seq[Cluster])] =
        Some((c.id, c.vehicles, c.commodities, c.subClusters))
}


@Entity
class Cluster() extends Model {
    @Id
    @Column(nullable = false)
    var id: String = null

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var vehicleList: util.List[Vehicle] = new util.ArrayList[Vehicle]()

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var commodityList: util.List[Commodity] = new util.ArrayList[Commodity]()

    def vehicles: Seq[Vehicle] = vehicleList.asScala

    def commodities: Seq[Commodity] = commodityList.asScala

    def subClusters: Seq[Cluster] = Cluster.byPrefix(id+"/")

    @Transient
    private val unsavedSubclusters: mutable.Buffer[Cluster] = mutable.Buffer.empty

    def parent: Option[Cluster] = Option(Cluster.finder.byId(id.substring(0,id.lastIndexOf("/"))))

    def parents: Iterator[Cluster] =
        Iterator.iterate[Option[Cluster]](
            this.parent
        )(
            _.flatMap(_.parent)
        ).takeWhile(_.isDefined).map(_.get)

    def descendants: Iterator[Cluster] =             // each level            // combine all the levels
        Iterator.iterate(subClusters.iterator)(_.map(_.subClusters).flatten).takeWhile(_.hasNext).flatten

    def application: Application =
        Application.finder.byId(id.iterator.takeWhile(_ != '/').mkString)

    @Transactional
    override def delete(): Boolean = {
        Cluster.byPrefix(id + "/").foreach(_.delete())
        super.delete()
    }

    @Transactional
    override def insert(): Unit = {
        unsavedSubclusters.foreach(_.insert())
        unsavedSubclusters.clear()
        super.insert()
    }

    @Transactional
    override def update(): Unit = {
        unsavedSubclusters.foreach(_.update())
        unsavedSubclusters.clear()
        super.update()
    }

    @Transactional
    override def save(): Unit = {
        unsavedSubclusters.foreach(_.save())
        unsavedSubclusters.clear()
        super.save()
    }

    override def toString = String.format(
        "Cluster(id: %s, vehicles: %s, commodities: %s)",
        id,
        util.Arrays.toString(vehicleList.toArray),
        util.Arrays.toString(commodityList.toArray))
}
