package io.pathfinder.websockets.controllers

import io.pathfinder.models.Commodity
import io.pathfinder.websockets.ModelTypes

object CommoditySocketController extends WebSocketCrudController[Long, Commodity](ModelTypes.Commodity,Commodity.Dao)
