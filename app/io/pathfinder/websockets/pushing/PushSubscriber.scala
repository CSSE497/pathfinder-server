package io.pathfinder.websockets.pushing

import akka.actor.ActorRef
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage.{UnsubscribeAll, Unsubscribe}

trait PushSubscriber {
  def subscribeByCluster(clusterId: Long, client: ActorRef): Unit

  def subscribeById(id: Long, client: ActorRef): Unit

  def unsubscribeById(id: Long, client: ActorRef): Unit

  def unsubscribeByClusterId(clusterId: Long, client: ActorRef): Unit

  def unsubscribe(client: ActorRef): Unit
}
