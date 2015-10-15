package io.pathfinder.websockets

import akka.event.ActorEventBus
import akka.event.ActorClassification
import akka.event.ActorClassifier

class NotificationPusher[+M] extends ActorEventBus with ActorClassification {
    override type Classifier = this.type

    override def subscribe(subscriber: NotificationPusher.this.type, to: Classifier): Boolean = ???

    override def publish(event: NotificationPusher.this.type): Unit = ???

    override def unsubscribe(subscriber: NotificationPusher.this.type, from: Classifier): Boolean = ???

    override def unsubscribe(subscriber: NotificationPusher.this.type): Unit = ???

    override type Event = this.type
    override type Subscriber = this.type
}
