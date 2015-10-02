package io.pathfinder.data;

import io.pathfinder.models.Commodity;

/** A wrapper around the Commodity Finder and constructor. */
public class CommodityDao extends EbeanCrudDao<Long,Commodity> {
    public CommodityDao() {
        super(Commodity.find);
    }
}
