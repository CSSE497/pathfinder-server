package io.pathfinder.models;

import com.avaje.ebean.annotation.EnumValue;

public enum VehicleStatus {
    @EnumValue("0")
    Offline,
    @EnumValue("1")
    Online
}
