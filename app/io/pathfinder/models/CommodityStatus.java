package io.pathfinder.models;

import com.avaje.ebean.annotation.EnumValue;
import io.pathfinder.util.JavaEnumFormat;
import play.api.libs.json.Format;

public enum CommodityStatus {
    @EnumValue("0")
    Inactive,
    @EnumValue("1")
    Cancelled,
    @EnumValue("2")
    Waiting,
    @EnumValue("3")
    PickedUp,
    @EnumValue("4")
    DroppedOff;

    public static final Format<CommodityStatus> format = new JavaEnumFormat<>(CommodityStatus.class);
}
