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

    def apply(id: String, name: String, defaultClusterId: Long): Application = {
        val app = new Application
        app.id = id
        app.cluster = Cluster.Dao.read(defaultClusterId).get
        app
    }

    def unapply(p: Application): Option[(String, String, Long)] =
        Some((p.id, p.name, Option(p.cluster).map(_.id).getOrElse(0L)))
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

    @ManyToOne
    @JoinColumn
    var cluster: Cluster = null

    def capacityParameters: mutable.Buffer[CapacityParameter] = capacityParametersList.asScala
    def objectiveParameters: mutable.Buffer[ObjectiveParameter] = objectiveParametersList.asScala
}
