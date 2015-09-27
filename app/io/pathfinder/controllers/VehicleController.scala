package io.pathfinder.controllers

import io.pathfinder.models.Vehicle
import Vehicle.{format,updateReads}

class VehicleController extends CrudController[Long,Vehicle](Vehicle.Dao)
