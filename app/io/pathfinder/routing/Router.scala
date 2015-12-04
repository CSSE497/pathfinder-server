package io.pathfinder.routing

import akka.actor.ActorRef
import akka.event.{LookupClassification, ActorEventBus}
import akka.util.Timeout
import io.pathfinder.config.Global
import io.pathfinder.models.{HasCluster, Cluster, Commodity, Vehicle}
import io.pathfinder.routing.ClusterRouter.ClusterRouterMessage
import io.pathfinder.routing.ClusterRouter.ClusterRouterMessage.{ClusterEvent, RouteRequest}
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage.Subscribe
import io.pathfinder.websockets.{Events, ModelTypes}
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * The Router is an object that is responsible for dispatching subscription requests and route updates to cluster routers.
 */
object Router extends ActorEventBus with LookupClassification {

    override type Classifier = Long // cluster id
    override type Event = (Long, Any) // cluster id and message to cluster router

    override protected def classify(event: Event): Classifier = event._1

    override protected def mapSize(): Int = 16

    override protected def publish(event: Event, subscriber: ActorRef): Unit = subscriber ! event._2

    def subscribeToRoute(client: ActorRef, model: ModelTypes.Value, id:Long): Boolean = {

        implicit val timeout = Timeout(2.seconds) // used for the recalculation futures used right below

        val cluster = model match {
            case ModelTypes.Vehicle =>
                Vehicle.Dao.read(id).getOrElse(return false).cluster
            case ModelTypes.Commodity =>
                Commodity.Dao.read(id).getOrElse(return false).cluster
            case ModelTypes.Cluster =>
                Cluster.Dao.read(id).getOrElse(return false)
        }
        if(subscribers.findValue(cluster.id)(x => true).isEmpty){
            val clusterRouter = Global.actorSystem.actorOf(ClusterRouter.props(cluster))
            subscribe(
                clusterRouter,
                cluster.id
            )
            cluster.descendants.foreach{
                parent => subscribe(clusterRouter, parent.id)
            }
        }
        publish((cluster.id, Subscribe(client, (model, id))))
        publish(cluster, RouteRequest(client, model, id))
        true
    }

    def publish(cluster: Cluster, msg: ClusterRouterMessage): Unit = {
        publish((cluster.id, msg))
    }

    def publish(event: Events.Value, model: HasCluster): Unit = {
        publish(model.cluster, ClusterEvent(event, model))
    }
}
