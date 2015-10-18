package io.pathfinder.websockets.pushing

import akka.actor.ActorRef

trait PushSubscriber {
  def subscribeByClusterId(clusterId: Long, client: ActorRef): Unit

  def subscribeById(id: Long, client: ActorRef): Unit

  def unsubscribeById(id: Long, client: ActorRef): Unit

  def unsubscribeByClusterId(clusterId: Long, client: ActorRef): Unit

  def unsubscribe(client: ActorRef): Unit
}
