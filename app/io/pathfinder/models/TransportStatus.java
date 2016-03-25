package io.pathfinder.models;

import com.avaje.ebean.annotation.EnumValue;
import io.pathfinder.util.JavaEnumFormat;
import play.api.libs.json.Format;

public enum TransportStatus {
    @EnumValue("0")
    Offline,
    @EnumValue("1")
    Online;

    public static final Format<TransportStatus> format = new JavaEnumFormat<>(TransportStatus.class);
}
