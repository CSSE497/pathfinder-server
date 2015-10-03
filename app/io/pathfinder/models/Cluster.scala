package io.pathfinder.models

import javax.persistence.{OneToMany, CascadeType, ManyToOne, Id, GenerationType, Column, GeneratedValue, Entity}

import com.avaje.ebean.Model
import io.pathfinder.data.{Resource, EbeanCrudDao}
import play.api.libs.json.{Json, Format}

object Cluster {
    val finder: Model.Find[Long, Cluster] = new Model.Finder[Long, Cluster](classOf[Cluster])
    object Dao extends EbeanCrudDao[Long, Cluster](finder)

    val DEFAULT_ID = 0;

    implicit val format: Format[Cluster] = Json.format[Cluster]
    implicit val resourceFormat: Format[ClusterResource] = Json.format[ClusterResource]

    case class ClusterResource(subClusters: Option[List[Long]], vehicles: Option[List[Long]], commodities: Option[List[Long]])
        extends Resource[Cluster] {
        /** Updates the specified model with the resource's fields. */
        override def update(model: Cluster): Option[Cluster] = {
            subClusters.foreach { ids => model.subClusters ++= ids.map(Cluster.finder.byId) }
            vehicles.foreach { ids => model.vehicles ++= ids.map(Vehicle.finder.byId) }
            commodities.foreach { ids => model.commodities ++= ids.map(Commodity.finder.byId) }
            Some(model)
        }

        /** Creates a new model instance from this resource. */
        override def create(): Option[Cluster] = {
            Some(Cluster(
                DEFAULT_ID,
                subClusters.getOrElse(List()),
                vehicles.getOrElse(List()),
                commodities.getOrElse(List())))
        }
    }

    def apply(id: Long, subClusters: List[Long], vehicles: List[Long], commodities: List[Long]): Cluster = {
        val c = new Cluster
        c.id = id
        c.subClusters = subClusters.map(Cluster.finder.byId)
        c.vehicles = vehicles.map(Vehicle.finder.byId)
        c.commodities = commodities.map(Commodity.finder.byId)
        c
    }

    def unapply(c: Cluster): Option[(Long, List[Long], List[Long], List[Long])] =
        Some((c.id, c.subClusters.map(_.id), c.vehicles.map(_.id), c.commodities.map(_.id)))
}

@Entity
class Cluster() extends Model {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @OneToMany(mappedBy="parent", cascade=Array(CascadeType.ALL))
    var subClusters: List[Cluster] = List()

    @OneToMany(mappedBy = "parent", cascade=Array(CascadeType.ALL))
    var vehicles: List[Vehicle] = List()

    @OneToMany(mappedBy = "parent", cascade=Array(CascadeType.ALL))
    var commodities: List[Commodity] = List()
}
