package io.pathfinder.websockets.controllers

import io.pathfinder.models.{Cluster, Commodity}
import io.pathfinder.websockets.ModelTypes

object ClusterSocketController extends WebSocketCrudController[Cluster](ModelTypes.Cluster,Cluster.Dao)