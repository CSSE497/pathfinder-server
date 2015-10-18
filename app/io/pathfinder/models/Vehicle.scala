package io.pathfinder.models


import akka.actor.{Props, ActorRef}
import com.avaje.ebean.Model
import io.pathfinder.websockets.{ModelTypes, WebSocketDao}
import javax.persistence.{ManyToOne, Id, Column, Entity, GeneratedValue, GenerationType}
import io.pathfinder.websockets.pushing.{SocketMessagePusher, ByIdPusher, EventBusActor, PushByClusterDao, PushByIdDao, ByClusterPusher}
import play.api.libs.json.{Writes, Format, Json}
import io.pathfinder.data.{ObserverDao, ClusterQueries, Resource}

object Vehicle {

    val finder: Model.Find[Long,Vehicle] = new Model.Finder[Long,Vehicle](classOf[Vehicle])

    object Dao extends ObserverDao[Vehicle](finder) with ClusterQueries[Long, Vehicle] with PushByIdDao[Vehicle] with PushByClusterDao[Vehicle] {
        
        class ByIdEventBus extends EventBusActor with SocketMessagePusher with ByIdPusher {
            override type Model = Vehicle
        }

        override def readByCluster(c: Cluster): Seq[Vehicle] = {
            c.refresh()
            c.vehicles
        }

        override def byIdEventBus: ActorRef

        override def byClusterEventBus: ActorRef = ???

        override def writer: Writes[Vehicle] = ???

        override def modelType: ModelTypes.Value = ???

        override def routers: ActorRef = ???
    }

    implicit val format: Format[Vehicle] = Json.format[Vehicle]

    implicit val resourceFormat: Format[VehicleResource] = Json.format[VehicleResource]

    case class VehicleResource(
        id:        Option[Long],
        latitude:  Option[Double],
        longitude: Option[Double],
        clusterId: Option[Long],
        capacity:  Option[Int]
    ) extends Resource[Vehicle] {
        override def update(v: Vehicle): Option[Vehicle] = {
            latitude.foreach(v.latitude = _)
            longitude.foreach(v.longitude = _)
            capacity.foreach(v.capacity = _)
            clusterId.foreach {
                Cluster.Dao.read(_).foreach(v.cluster = _)
            }
            Some(v)
        }

        def create(c: Cluster): Option[Vehicle] = {
            for {
                lat <- latitude
                lng <- longitude
                cap <- capacity
            } yield {
                val v = Vehicle(id.getOrElse(0),lat,lng,cap)
                v.cluster = c
                v
            }
        }

        override def create: Option[Vehicle] = for {
            id <- clusterId
            cluster <- Cluster.Dao.read(id)
            mod <- create(cluster)
        } yield mod
    }

    def apply(id: Long, latitude: Double, longitude: Double, capacity: Int): Vehicle = {
        val v = new Vehicle
        v.id = id
        v.latitude = latitude
        v.longitude = longitude
        v.capacity = capacity
        v
    }

    def unapply(v: Vehicle): Option[(Long, Double, Double, Int)] = Some((v.id, v.latitude, v.longitude, v.capacity))
}

@Entity
class Vehicle() extends Model with HasId with HasCluster {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable=false)
    var latitude: Double = 0

    @Column(nullable=false)
    var longitude: Double = 0

    @Column(nullable=false)
    var capacity: Int = 0

    @ManyToOne
    @Column
    var cluster: Cluster = null
}
