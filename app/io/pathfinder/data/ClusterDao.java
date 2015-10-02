package io.pathfinder.data;

import io.pathfinder.models.Cluster;

/** A wrapper around the Cluster constructor and Finder. */
public class ClusterDao extends EbeanCrudDao<Long,Cluster> {
    public ClusterDao() {
        super(Cluster.find);
    }
}
