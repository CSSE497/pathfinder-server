package io.pathfinder.websockets.pushing

import akka.actor.{Props, ActorRef}
import akka.event.{LookupClassification, ActorEventBus}
import play.Logger

object SocketMessagePusher {
    def props: Props = Props(classOf[SocketMessagePusher])
}

class SocketMessagePusher extends EventBusActor with ActorEventBus with LookupClassification {

    override type Event = (String, Any) // id and message

    override type Classifier = String // cluster id

    override protected def classify(event: Event): Classifier = event._1

    override protected def publish(event: Event, subscriber: ActorRef): Unit = {
        Logger.info("pushing: "+event._2+ " to websocket: "+subscriber)
        subscriber ! event._2
    }

    override def publish(event: Event): Unit = {
        Logger.info("notification pushed: "+event)
        super.publish(event)
    }

    override protected def mapSize(): Int = 16

    override def subscribe(ref: ActorRef, classifier: Classifier): Boolean = {
        Logger.info("websocket "+ref+" subscribed to: "+classifier+" at "+self)
        super.subscribe(ref, classifier)
    }

    override def unsubscribe(ref: ActorRef, classifier: Classifier): Boolean = {
        Logger.info("Websocket "+ref+" unsubscribed from: "+classifier+" for "+self)
        super.unsubscribe(ref, classifier)
    }

    override def unsubscribe(ref: ActorRef): Unit = {
        Logger.info("Websocket "+ref+" unsubscribed from: "+self)
        super.unsubscribe(ref)
    }
}
