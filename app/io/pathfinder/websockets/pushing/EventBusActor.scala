package io.pathfinder.websockets.pushing

import akka.actor.{Actor, ActorRef}
import akka.event.ActorEventBus
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage.{UnsubscribeAll, Unsubscribe, Publish, Subscribe}

object EventBusActor {
    abstract sealed class EventBusMessage

    object EventBusMessage {
        case class Subscribe[C](subscriber: ActorRef, to: C) extends EventBusMessage
        case class Unsubscribe[C](subscriber: ActorRef, from: C) extends EventBusMessage
        case class UnsubscribeAll(subscriber: ActorRef) extends EventBusMessage
        case class Publish[E](event: E) extends EventBusMessage
    }
}

abstract class EventBusActor extends Actor with ActorEventBus {

    override def receive: Receive = {
        case Subscribe(sub, to)     => subscribe(sub, to.asInstanceOf[Classifier])
        case Unsubscribe(sub, from) => unsubscribe(sub, from.asInstanceOf[Classifier])
        case UnsubscribeAll(sub)    => unsubscribe(sub)
        case Publish(event)         => publish(event.asInstanceOf[Event])
    }
}
