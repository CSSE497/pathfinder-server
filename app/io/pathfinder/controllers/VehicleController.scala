package io.pathfinder.controllers

import io.pathfinder.models.Vehicle
import io.pathfinder.data.EbeanCrudDao

class VehicleController extends CrudController[Long,Vehicle](Vehicle, new EbeanCrudDao(Vehicle.finder))
