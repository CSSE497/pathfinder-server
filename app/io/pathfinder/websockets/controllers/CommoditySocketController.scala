package io.pathfinder.websockets.controllers

import io.pathfinder.models.Commodity
import io.pathfinder.websockets.ModelTypes

object CommoditySocketController extends WebSocketCrudController[Commodity](ModelTypes.Commodity,Commodity.Dao)
