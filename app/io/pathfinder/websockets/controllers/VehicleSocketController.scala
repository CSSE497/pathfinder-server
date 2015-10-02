package io.pathfinder.websockets.controllers

import io.pathfinder.models.Vehicle
import io.pathfinder.websockets.ModelTypes

object VehicleSocketController extends WebSocketCrudController[Vehicle](ModelTypes.Vehicle,Vehicle.Dao)
