package io.pathfinder.websockets

import io.pathfinder.util.HasFormat

/**
 * An enum containing the models that a websocket message can use
 */
object ModelTypes extends Enumeration with HasFormat {
    type ModelType = Value
    val Transport, Commodity, Cluster = Value
}
