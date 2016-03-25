package io.pathfinder.models

import java.util
import javax.persistence.{Transient, OneToMany, CascadeType, Id, Column, Entity}

import com.avaje.ebean.Model
import com.avaje.ebean.annotation.Transactional
import io.pathfinder.data.{EbeanCrudDao, Resource}
import play.api.libs.json.{Json, Format}
import scala.collection.JavaConverters.{asScalaBufferConverter, seqAsJavaListConverter, asScalaIteratorConverter}
import scala.collection.{mutable, Iterator}

object Cluster {
    private val ROOT: String = "/root"

    val finder: Model.Find[String, Cluster] = new Model.Finder[String, Cluster](classOf[Cluster])

    def byPrefix(path: String): Iterator[Cluster] =
        finder.query().where().startsWith("id", path).findIterate().asScala

    object Dao extends EbeanCrudDao[String, Cluster](finder)

    def addAppToPath(path: String, app: String): Option[String] =
        if (path.startsWith(ROOT)) {
            Some(app + path.stripPrefix(ROOT))
        } else None

    def removeAppFromPath(path: String): String =
        if(path.startsWith(ROOT)) {
            path
        } else {
            val i = path.indexOf("/")
            if(i == -1){
                ROOT
            } else {
                ROOT + path.substring(i)
            }
        }

    implicit val format: Format[Cluster] = Json.format[Cluster]
    implicit val resourceFormat: Format[ClusterResource] = Json.format[ClusterResource]

    case class ClusterResource(
        id: Option[String],
        transports: Option[Seq[Transport.TransportResource]],
        commodities: Option[Seq[Commodity.CommodityResource]],
        subClusters: Option[Seq[ClusterResource]]
    ) extends Resource[Cluster] {
        override type R = ClusterResource

        override def update(model: Cluster): Option[Cluster] = None // Cluster Updates are not supported

        /** Creates a new model instance from this resource. */
        override def create: Option[Cluster] = {
            val model = new Cluster
            model.id = id.getOrElse(return None) // should validate path
            transports.foreach(
                _.foreach {
                    transportResource => model.transportList.add(
                        (for {
                            id <- transportResource.id
                            model <- Transport.Dao.read(id)
                            update <- transportResource.update(model)
                        } yield update).orElse(transportResource.create(model)).getOrElse(return None)
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

        override def withAppId(appId: String): Option[ClusterResource] = Some(
            copy(
                id.map(Cluster.addAppToPath(_, appId).getOrElse(return None)),
                transports.map(_.map(_.withAppId(appId).getOrElse(return None))),
                commodities.map(_.map(_.withAppId(appId).getOrElse(return None))),
                subClusters.map(_.map(_.withAppId(appId).getOrElse(return None)))
            )
        )

        override def withoutAppId: ClusterResource = copy(
            id.map(Cluster.removeAppFromPath),
            transports.map(_.map(_.withoutAppId)),
            commodities.map(_.map(_.withoutAppId)),
            subClusters.map(_.map(_.withoutAppId))
        )
    }

    def apply(id: String, transports: Seq[Transport], commodities: Seq[Commodity], subClusters: Seq[Cluster]): Cluster = {
        val c = new Cluster
        c.id = id
        c.transportList.addAll(transports.asJava)
        c.commodityList.addAll(commodities.asJava)
        c.unsavedSubclusters ++= subClusters
        c
    }

    def unapply(c: Cluster): Option[(String, Seq[Transport], Seq[Commodity], Seq[Cluster])] =
        Some((Cluster.removeAppFromPath(c.id), c.transports, c.commodities, c.subClusters.toSeq))
}

@Entity
class Cluster() extends Model {
    @Id
    @Column(nullable = false)
    var id: String = null

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var transportList: util.List[Transport] = new util.ArrayList[Transport]()

    @OneToMany(mappedBy = "cluster", cascade=Array(CascadeType.ALL))
    var commodityList: util.List[Commodity] = new util.ArrayList[Commodity]()

    def transports: Seq[Transport] = transportList.asScala

    def commodities: Seq[Commodity] = commodityList.asScala

    def subClusters: Iterator[Cluster] = descendants.filter(_.id.count(_ == '/') == id.count(_ == '/') + 1)

    @Transient
    private val unsavedSubclusters: mutable.Buffer[Cluster] = mutable.Buffer.empty

    def parent: Option[Cluster] = Option(Cluster.finder.byId(id.substring(0,id.lastIndexOf("/"))))

    def parents: Iterator[Cluster] =
        Iterator.iterate[Option[Cluster]](
            this.parent
        )(
            _.flatMap(_.parent)
        ).takeWhile(_.isDefined).map(_.get)

    def descendants: Iterator[Cluster] = Cluster.byPrefix(id +"/")

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
        "Cluster(id: %s, transports: %s, commodities: %s)",
        id,
        util.Arrays.toString(transportList.toArray),
        util.Arrays.toString(commodityList.toArray))
}
