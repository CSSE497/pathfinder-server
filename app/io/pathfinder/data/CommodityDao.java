package io.pathfinder.data;

import io.pathfinder.models.Commodity;

/** A wrapper around the Commodity Finder and constructor. */
public class CommodityDao extends EbeanCrudDao<Long,Commodity> {
    public CommodityDao() {
        super(Commodity.find);
    }

    /**
     * Creates a default Commodity. In practice, this should never be used because every commodity
     * should be initialized with a parent. The database will prevent you from saving this object
     * if you try.
     */
    @Override
    public Commodity construct() {
        return new Commodity();
    }
}
