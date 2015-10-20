package io.pathfinder.models

import java.util
import java.util.UUID
import javax.persistence.{CascadeType, OneToMany, JoinColumn, Column, OneToOne, Id, Entity}
import com.avaje.ebean.Model
import com.avaje.ebean.Model.{Finder, Find}
import play.api.libs.json.{Writes, Json, Format}

import scala.collection.mutable
import scala.collection.JavaConverters.asScalaBufferConverter


object PathFinderApplication {
    val finder: Find[String, PathFinderApplication] =
        new Finder[String, PathFinderApplication](classOf[PathFinderApplication])

    val format: Format[PathFinderApplication] = Json.format[PathFinderApplication]

    def apply(id: UUID, name: String, defaultClusterId: Long, clusterIds: Seq[Long]): PathFinderApplication = {
        val app = new PathFinderApplication
        app.id = id
        app.defaultCluster = Cluster.Dao.read(defaultClusterId).get
        Cluster.Dao.readAll(clusterIds).fold(
            id => throw new IllegalArgumentException("No Cluster with id: " + id),
            app.clusters ++= _
        )
        app
    }

    def unapply(p: PathFinderApplication): Option[(UUID, String, Long, Seq[Long])] =
        Some((p.id, p.name, Option(p.defaultCluster).map(_.id).getOrElse(0L), p.clusters.map(_.id)))
}

@Entity
class PathFinderApplication extends Model {

    @Id
    @Column(updatable = false)
    var id: UUID = null

    @Column
    var name: String = null

    @OneToOne
    @JoinColumn
    var defaultCluster: Cluster = null

    @OneToMany(cascade=Array(CascadeType.ALL))
    var clusterList: util.List[Cluster] = new util.ArrayList[Cluster]()

    def clusters: mutable.Buffer[Cluster] = clusterList.asScala
}
