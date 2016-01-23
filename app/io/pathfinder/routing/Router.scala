package io.pathfinder.routing

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.event.{SubchannelClassification, ActorEventBus}
import akka.util.{Subclassification, Timeout}
import io.pathfinder.config.Global
import io.pathfinder.models.{ModelId, HasCluster, Cluster, Commodity, Vehicle}
import io.pathfinder.routing.ClusterRouter.ClusterRouterMessage
import io.pathfinder.routing.ClusterRouter.ClusterRouterMessage.{RouteRequest, ClusterEvent}
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage.Subscribe
import io.pathfinder.websockets.Events
import play.Logger
import scala.collection.mutable
import scala.concurrent.duration.{FiniteDuration, DurationInt}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * The Router is an object that is responsible for dispatching subscription requests and route updates to cluster routers.
 */
object Router extends ActorEventBus with SubchannelClassification {

    override type Classifier = String // cluster path
    override type Event = (String, Any) // cluster path and message to cluster router
    override type Subscriber = ActorRef

    // SubchannelClassification is retarded and didn't provide a way to view the current subscriptions

    private def add(path: String): Boolean = {
        val ref = Global.actorSystem.actorOf(ClusterRouter.props(path))
        if(subscribe(ref, path)){
            subs.put(path, ref)
            true
        } else {
            false
        }
    }

    private def remove(path: String): Boolean = {
        val ref = subs.get(path).orElse(return false).get
        unsubscribe(ref)
        true
    }

    protected val subs: mutable.Map[String, ActorRef] = new mutable.HashMap[String, ActorRef]

    override protected val subclassification = new Subclassification[Classifier] {
        override def isEqual(x: String, y: String): Boolean = x.equals(y)

        override def isSubclass(x: String, y: String): Boolean = y.startsWith(x)
    }

    override protected def classify(event: Event): Classifier = event._1

    override protected def publish(event: Event, subscriber: ActorRef): Unit = subscriber ! event._2

    private def clusterFromId(id: ModelId): Option[Cluster] = id match {
        case ModelId.VehicleId(vId) =>
            Vehicle.Dao.read(vId).map(_.cluster)
        case ModelId.CommodityId(cId) =>
            Commodity.Dao.read(cId).map(_.cluster)
        case ModelId.ClusterPath(path) =>
            Cluster.Dao.read(path)
    }

    def subscribeToRoute(client: ActorRef, id: ModelId): Boolean = {
        implicit val timeout = Timeout(2.seconds) // used for the recalculation futures used right below

        val cluster = clusterFromId(id).getOrElse(return false)
        if(!subs.contains(cluster.id)){
            add(cluster.id)
        }
        publish((cluster.id, Subscribe(client, id)))
        publish(cluster, RouteRequest(client, id))
        true
    }

    def routeRequest(client: ActorRef, id: ModelId): Boolean = {
        val cluster = clusterFromId(id).getOrElse(return false)
        if(!subs.contains(cluster.id)){
            add(cluster.id)
        }
        publish(cluster, RouteRequest(client, id))
        true
    }

    def publish(cluster: Cluster, msg: ClusterRouterMessage): Unit = {
        publish((cluster.id, msg))
    }

    def publish(event: Events.Value, model: HasCluster): Unit = {
        publish(model.cluster, ClusterEvent(event, model))
    }
}
