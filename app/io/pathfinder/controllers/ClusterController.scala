package io.pathfinder.controllers

import io.pathfinder.models.Cluster
import io.pathfinder.websockets.ModelTypes

class ClusterController extends CrudController[String, Cluster](Cluster.Dao) {
    override val model = ModelTypes.Cluster
}
