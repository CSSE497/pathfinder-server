package io.pathfinder.routing

import akka.actor.{Props, ActorRef}
import akka.event.{LookupClassification, ActorEventBus}
import akka.pattern.ask
import akka.util.Timeout
import io.pathfinder.config.Global
import io.pathfinder.models.{Cluster, Commodity, Vehicle}
import io.pathfinder.routing.ClusterRouter.Recalculate
import io.pathfinder.websockets.pushing.EventBusActor
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage.{Subscribe, Publish}
import io.pathfinder.websockets.{ModelTypes, Events}
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

object Router {

    // This can be used to force the initialization of the static code in this class from Java.
    def init(): Unit = {}

    // all other codes should use this value to communicate with the router actor
    val ref: ActorRef = Global.actorSystem.actorOf(Props(classOf[Router]))
    implicit val timeout = Timeout(2.seconds) // used for the recalculation futures used right below

    // TODO: actually check that subscriptions are made correctly, may require rewriting code to use Futures
    object RouteSubscriber {
        def subscribe(client: ActorRef, model: ModelTypes.Value, id: Long): Boolean = {
            val cluster = model match {
                case ModelTypes.Vehicle =>
                    Vehicle.Dao.read(id).getOrElse(return false).cluster
                case ModelTypes.Commodity =>
                    Commodity.Dao.read(id).getOrElse(return false).cluster
                case ModelTypes.Cluster =>
                    (ref ? Publish(id, Subscribe(client, (ModelTypes.Cluster, id)))).onComplete{
                        x => ref ! Publish((id, Recalculate)) // recalculate to send pushes to newly registered (as well as old)
                                               // websockets, this really should be replaced with something less silly
                    }
                    return true
            }
            (ref ? Publish(cluster.id, Subscribe(client, (model, id)))).onComplete{x => ref ! Publish((cluster.id, Recalculate))} // read above
            true
        }
    }
}

/**
 * The Router is an actor that is responsible for dispatching subscription requests and route updates to cluster routers.
 */
class Router extends EventBusActor with ActorEventBus with LookupClassification {

    if(Router.ref != self){
        throw new Error("Router Actor must be a singleton")
    }

    // populate the cluster routers
    Cluster.Dao.readAll.foreach{
        cluster =>
            subscribe(
                Global.actorSystem.actorOf(ClusterRouter.props(cluster)),
                cluster.id
            )
    }

    override def receive: Receive = {
        case (Events.Created, cluster: Cluster) =>
            subscribe(Global.actorSystem.actorOf(ClusterRouter.props(cluster)), cluster.id)
        case (Events.Deleted, cluster: Cluster) =>
            subscribers.remove(cluster.id).foreach(
                _.foreach(Global.actorSystem.stop)
            )
        case Publish((id: Long, Recalculate)) => publish((id,Right(Recalculate)))
        case Publish((id: Long, msg: EventBusMessage)) => publish((id,Left(msg)))
        case _Else => super.receive(_Else)
    }

    override type Classifier = Long // cluster id
    override type Event = (Long, Either[EventBusMessage,Recalculate.type]) // cluster id and cluster router message

    override protected def classify(event: Event): Classifier = event._1

    override protected def mapSize(): Int = 16

    override protected def publish(event: Event, subscriber: ActorRef): Unit = subscriber ! event._2
}
