package io.pathfinder.models

import java.util.UUID
import javax.persistence.{ManyToOne, JoinColumn, Column, Id, Entity}
import com.avaje.ebean.Model
import com.avaje.ebean.Model.{Finder, Find}
import play.api.libs.json.{Json, Format}



object Application {
    val finder: Find[UUID, Application] =
        new Finder[UUID, Application](classOf[Application])

    val format: Format[Application] = Json.format[Application]

    def apply(id: UUID, name: String, token: Array[Byte], defaultClusterId: Long): Application = {
        val app = new Application
        app.id = id
        app.token = token
        app.cluster = Cluster.Dao.read(defaultClusterId).get
        app
    }

    def unapply(p: Application): Option[(UUID, String, Array[Byte], Long)] =
        Some((p.id, p.name, p.token, Option(p.cluster).map(_.id).getOrElse(0L)))
}

@Entity
class Application extends Model {

    @Id
    @Column(updatable = false)
    var id: UUID = null

    @Column
    var name: String = null

    @Column
    var token: Array[Byte] = "SECRET TOKEN".getBytes

    @ManyToOne
    @JoinColumn
    var cluster: Cluster = null
}
