package io.pathfinder.models

import java.util.UUID
import javax.persistence.{ManyToOne, JoinColumn, Column, Id, Entity}
import com.avaje.ebean.Model
import com.avaje.ebean.Model.{Finder, Find}
import play.api.libs.json.{Json, Format}



object PathfinderApplication {
    val finder: Find[UUID, PathfinderApplication] =
        new Finder[UUID, PathfinderApplication](classOf[PathfinderApplication])

    val format: Format[PathfinderApplication] = Json.format[PathfinderApplication]

    def apply(id: UUID, name: String, defaultClusterId: Long): PathfinderApplication = {
        val app = new PathfinderApplication
        app.id = id
        app.cluster = Cluster.Dao.read(defaultClusterId).get
        app
    }

    def unapply(p: PathfinderApplication): Option[(UUID, String, Long)] =
        Some((p.id, p.name, Option(p.cluster).map(_.id).getOrElse(0L)))
}

@Entity
class PathfinderApplication extends Model {

    @Id
    @Column(updatable = false)
    var id: UUID = null

    @Column
    var name: String = null

    @ManyToOne
    @JoinColumn
    var cluster: Cluster = null
}
