package io.pathfinder.websockets.controllers

import io.pathfinder.models.{Commodity, Cluster, Transport}
import io.pathfinder.routing.Action.{Start, DropOff, PickUp}
import io.pathfinder.routing.{Action, Route}
import io.pathfinder.websockets.{WebSocketMessage, ModelTypes}
import io.pathfinder.websockets.WebSocketMessage.{Route => RouteMsg, Error, Subscribe, Routed}
import scala.collection.JavaConversions.asScalaBuffer
/**
 * manages vehicle API calls
 */
object VehicleSocketController extends WebSocketCrudController[Long, Transport](ModelTypes.Transport,Transport.Dao)

