package io.pathfinder.data

import com.avaje.ebean.Model
import io.pathfinder.models.Cluster

trait ClusterQueries[K,V <: Model] extends EbeanCrudDao[K,V] {
    def readByCluster(c : Cluster): Seq[V]
    def readByCluster(clusterId: Long): Seq[V] = readByCluster(Cluster.finder.byId(clusterId))
}
