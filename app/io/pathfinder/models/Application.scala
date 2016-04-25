package io.pathfinder.models

import java.util
import javax.persistence.CascadeType._
import javax.persistence._
import com.avaje.ebean.Model
import com.avaje.ebean.Model.{Finder, Find}
import play.api.libs.json.{Json, Format}

import scala.collection.mutable
import scala.collection.JavaConverters.asScalaBufferConverter

object Application {
    val finder: Find[String, Application] =
        new Finder[String, Application](classOf[Application])

    val format: Format[Application] = Json.format[Application]

    def apply(id: String, name: String): Application = {
        val app = new Application
        app.id = id
        app
    }

    def unapply(p: Application): Option[(String, String)] =
        Some((p.id, p.name))
}

@Entity
class Application extends Model {

    @Id
    @Column(updatable = false)
    var id: String = null

    @Column
    var name: String = null

    @ManyToOne
    var customer: Customer = null

    @OneToMany(mappedBy = "application", cascade = Array(ALL))
    var capacityParametersList: util.List[CapacityParameter] = new util.ArrayList[CapacityParameter]()

    @OneToMany(mappedBy = "application", cascade = Array(ALL))
    var objectiveParametersList: util.List[ObjectiveParameter] = new util.ArrayList[ObjectiveParameter]()

    @ManyToOne
    var objectiveFunction: ObjectiveFunction = null

    @Column
    var key: Array[Byte] = null

    @Column
    var auth_url: String = null

    def cluster: Cluster = {
        Cluster.finder.byId(id)
    }

    def capacityParameters: mutable.Buffer[CapacityParameter] = capacityParametersList.asScala
    def objectiveParameters: mutable.Buffer[ObjectiveParameter] = objectiveParametersList.asScala
}
