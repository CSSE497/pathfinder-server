package io.pathfinder.websockets.pushing

import akka.actor.{Props, ActorRef}
import akka.event.{LookupClassification, ActorEventBus}
import io.pathfinder.websockets.WebSocketMessage

object SocketMessagePusher {
    def props[K]: Props = Props(classOf[SocketMessagePusher[K]])
}

class SocketMessagePusher[K] extends EventBusActor with ActorEventBus with LookupClassification {

    override type Event = (K, WebSocketMessage) // id and message

    override type Classifier = K // cluster id

    override protected def classify(event: Event): Classifier = event._1

    override protected def publish(event: Event, subscriber: ActorRef): Unit =
        subscriber ! event._2

    override protected def mapSize(): Int = 16
}
