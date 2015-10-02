package io.pathfinder.data;

import io.pathfinder.models.Cluster;

/** A wrapper around the Cluster constructor and Finder. */
public class ClusterDao extends EbeanCrudDao<Long,Cluster> {
    public ClusterDao() {
        super(Cluster.find);
    }

    /**
     * Creates a default Cluster. In practice, this should never be used because every cluster
     * should be initialized with a parent and a link to it's minting credentials. The database
     * will prevent you from saving this object if you try.
     */
    @Override
    public Cluster construct() {
        return new Cluster();
    }
}
