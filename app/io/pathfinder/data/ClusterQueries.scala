package io.pathfinder.data

import com.avaje.ebean.Model
import io.pathfinder.models.Cluster

/**
 * Provides convenience methods for cluster-level queries on CrudDao. This allows us to easily get all Models by
 * cluster.
 */
trait ClusterQueries[K,V <: Model] extends CrudDao[K,V] {
    def readByCluster(c : Cluster): Seq[V]
    def readByCluster(clusterId: Long): Seq[V] = readByCluster(Cluster.finder.byId(clusterId))
}
