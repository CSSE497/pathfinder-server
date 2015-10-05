package io.pathfinder.models

import com.avaje.ebean.Model

trait HasParent extends Model {
    def parent: Cluster
}
