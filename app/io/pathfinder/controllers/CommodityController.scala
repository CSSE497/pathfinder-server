package io.pathfinder.controllers

import io.pathfinder.models.Commodity
import io.pathfinder.websockets.ModelTypes

class CommodityController extends CrudController[Long,Commodity](Commodity.Dao){
    override val model = ModelTypes.Commodity
}
