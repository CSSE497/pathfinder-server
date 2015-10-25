package io.pathfinder.models

import java.util.UUID
import javax.persistence.{ManyToOne, JoinColumn, Column, Id, Entity}
import com.avaje.ebean.Model
import com.avaje.ebean.Model.{Finder, Find}
import play.api.libs.json.{Json, Format}



object PathFinderApplication {
    val finder: Find[UUID, PathFinderApplication] =
        new Finder[UUID, PathFinderApplication](classOf[PathFinderApplication])

    val format: Format[PathFinderApplication] = Json.format[PathFinderApplication]

    def apply(id: UUID, name: String, defaultClusterId: Long): PathFinderApplication = {
        val app = new PathFinderApplication
        app.id = id
        app.cluster = Cluster.Dao.read(defaultClusterId).get
        app
    }

    def unapply(p: PathFinderApplication): Option[(UUID, String, Long)] =
        Some((p.id, p.name, Option(p.cluster).map(_.id).getOrElse(0L)))
}

@Entity
class PathFinderApplication extends Model {

    @Id
    @Column(updatable = false)
    var id: UUID = null

    @Column
    var name: String = null

    @ManyToOne
    @JoinColumn
    var cluster: Cluster = null
}
