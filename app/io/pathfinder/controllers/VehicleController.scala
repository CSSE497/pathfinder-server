package io.pathfinder.controllers

import io.pathfinder.models.Vehicle
import Vehicle.{format,resourceFormat}
import io.pathfinder.websockets.ModelTypes
import io.pathfinder.websockets.ModelTypes.ModelType

class VehicleController extends CrudController[Long,Vehicle](Vehicle.Dao) {
    override val model: ModelType = ModelTypes.Vehicle
}
