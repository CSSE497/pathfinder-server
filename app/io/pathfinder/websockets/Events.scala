package io.pathfinder.websockets

import io.pathfinder.util.HasFormat

/**
 * the valid events that a client may subscribe to
 */
object Events extends Enumeration with HasFormat {
  type Event = Value
  val Created, Updated, Deleted = Value
}
