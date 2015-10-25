package io.pathfinder.models

import com.avaje.ebean.Model

trait HasCluster extends Model {
    def cluster: Cluster
}
