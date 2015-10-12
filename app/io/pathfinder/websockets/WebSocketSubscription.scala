package io.pathfinder.websockets

import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Iteratee, Concurrent, Enumerator}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * This class holds a single subscription for a websocket
 * TODO: add cluster support
 */
class WebSocketSubscription(client: Channel[WebSocketMessage], source: Enumerator[WebSocketMessage]) {
  private val (enumerator, channel) = Concurrent.broadcast(source)
  enumerator(Iteratee.foreach(client.push))

  def unSubscribe(): Unit = channel.close()
}
