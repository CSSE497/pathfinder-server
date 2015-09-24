package io.pathfinder.controllers

import io.pathfinder.models.Vehicle
import io.pathfinder.data.EbeanCrudDao
import Vehicle.{format,updateReads}

class VehicleController extends CrudController[Long,Vehicle](Vehicle.Dao)
