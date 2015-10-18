package io.pathfinder.routing

import akka.actor.{Props, ActorRef}
import akka.event.{LookupClassification, ActorEventBus}
import com.avaje.ebean.Model
import io.pathfinder.config.Global
import io.pathfinder.websockets.pushing.EventBusActor
import io.pathfinder.websockets.{ModelTypes, Events}

object Router {

    // This can be used to force the initialization of the static code in this class from Java.
    def init(): Unit = {}
    val ref: ActorRef = Global.actorSystem.actorOf(Props(classOf[Router]))
}

class Router extends EventBusActor with ActorEventBus with LookupClassification {

    override type Classifier = Long
    override type Event = (Long, (ModelTypes.Value, Events.Value, Model))

    override protected def classify(event: Event): Classifier = event._1

    override protected def mapSize(): Int = 16

    override protected def publish(event: Event, subscriber: ActorRef): Unit = subscriber ! event
}
