package io.pathfinder.routing

import akka.actor.{Props, ActorRef}
import akka.event.{LookupClassification, ActorEventBus}
import com.avaje.ebean.Model
import io.pathfinder.config.Global
import io.pathfinder.models.{Commodity, Vehicle}
import io.pathfinder.websockets.pushing.EventBusActor
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage.{Subscribe, Publish}
import io.pathfinder.websockets.{ModelTypes, Events}

object Router {

    // This can be used to force the initialization of the static code in this class from Java.
    def init(): Unit = {}
    abstract sealed class RouterMessage
    val ref: ActorRef = Global.actorSystem.actorOf(Props(classOf[Router]))
    object RouteSubscriber {
        def subscribe(client: ActorRef, model: ModelTypes.Value, id: Long): Boolean = {
            val cluster = model match {
                case ModelTypes.Vehicle =>
                    Vehicle.Dao.read(id).getOrElse(return false).cluster
                case ModelTypes.Commodity =>
                    Commodity.Dao.read(id).getOrElse(return false).cluster
                case ModelTypes.Cluster =>
                    client ! Publish(id, Subscribe(client, (ModelTypes.Cluster, id)))
                    return true
            }
            client ! Publish(cluster.id, Subscribe(client, (model, id)))
            true
        }
    }
}

class Router extends EventBusActor with ActorEventBus with LookupClassification {

    override type Classifier = Long // cluster id
    override type Event = (Long, EventBusMessage) // cluster id and message

    override protected def classify(event: Event): Classifier = event._1

    override protected def mapSize(): Int = 16

    override protected def publish(event: Event, subscriber: ActorRef): Unit = subscriber ! event

    override def receive: Receive = {
        case Publish((id: Long, event: Events.Value, model: Model)) => publish(id, event, model)
        case _Else => super.receive(_Else)
    }

    def publish(clusterId: Long, event: Events.Value, model: Model): Unit ={
        publish((clusterId, Publish((event, model))))
    }
}
