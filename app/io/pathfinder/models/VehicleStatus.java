package io.pathfinder.models;

import com.avaje.ebean.annotation.EnumValue;
import io.pathfinder.util.JavaEnumFormat;
import play.api.libs.json.Format;

public enum VehicleStatus {
    @EnumValue("0")
    Offline,
    @EnumValue("1")
    Online;

    public static final Format<VehicleStatus> format = new JavaEnumFormat<>(VehicleStatus.class);
}
