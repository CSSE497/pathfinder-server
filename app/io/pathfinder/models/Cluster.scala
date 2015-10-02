package io.pathfinder.models

import javax.persistence.{OneToMany, CascadeType, ManyToOne, Id, GenerationType, Column, GeneratedValue, Entity}

import com.avaje.ebean.Model
import com.fasterxml.jackson.annotation.JsonIgnore
import io.pathfinder.data.{Resource, EbeanCrudDao}
import play.api.libs.json.{Json, Format}

object Cluster {
    val finder: Model.Find[Long, Cluster] = new Model.Finder[Long, Cluster](classOf[Cluster])
    object Dao extends EbeanCrudDao[Long, Cluster](finder)

    val DEFAULT_ID = 0;

    implicit val format: Format[Cluster] = Json.format[Cluster]
    implicit val resourceFormat: Format[ClusterResource] = Json.format[ClusterResource]

    case class ClusterResource(parentId: Option[Long], subClusters: Option[List[Long]], vehicles: Option[List[Long]], commodities: Option[List[Long]])
        extends Resource[Cluster] {
        /** Updates the specified model with the resource's fields. */
        override def update(model: Cluster): Option[Cluster] = {
            parentId.foreach { id => model.parent = Cluster.finder.byId(id) }
            subClusters.foreach { ids => model.subClusters ++= ids.map(Cluster.finder.byId) }
            vehicles.foreach { ids => model.vehicles ++= ids.map(Vehicle.finder.byId) }
            commodities.foreach { ids => model.commodities ++= ids.map(Commodity.finder.byId) }
            Some(model)
        }

        /** Creates a new model instance from this resource. */
        override def create(): Option[Cluster] = {
            Some(Cluster(
                DEFAULT_ID,
                parentId.getOrElse(-1),
                subClusters.getOrElse(List()),
                vehicles.getOrElse(List()),
                commodities.getOrElse(List())))
        }
    }

    def apply(id: Long, parentId: Long, subClusters: List[Long], vehicles: List[Long], commodities: List[Long]): Cluster = {
        val c = new Cluster
        c.id = id
        if (parentId > 0) {
            c.parent = Cluster.finder.byId(parentId)
        }
        c.subClusters = subClusters.map(Cluster.finder.byId)
        c.vehicles = vehicles.map(Vehicle.finder.byId)
        c.commodities = commodities.map(Commodity.finder.byId)
        c
    }

    def unapply(c: Cluster): Option[(Long, Long, List[Long], List[Long], List[Long])] =
        Some((c.id, (if (c.parent != null) c.parent.id else null).asInstanceOf[Long],
            c.subClusters.map(_.id), c.vehicles.map(_.id), c.commodities.map(_.id)))
}

@Entity
class Cluster() extends Model {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @JsonIgnore
    @ManyToOne
    var parent: Cluster = null

    @OneToMany(mappedBy="parent", cascade=Array(CascadeType.ALL))
    var subClusters: List[Cluster] = List()

    @OneToMany(mappedBy = "parent", cascade=Array(CascadeType.ALL))
    var vehicles: List[Vehicle] = List()

    @OneToMany(mappedBy = "parent", cascade=Array(CascadeType.ALL))
    var commodities: List[Commodity] = List()
}
