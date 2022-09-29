package appland.telemetry.data;

import org.jetbrains.annotations.NotNull;

import lombok.Getter;

public enum Tag {
    OsVersion("ai.device.osVersion"),
    UserId("ai.user.id"),
    SessionId("ai.session.id");

    @Getter
    private final String id;

    Tag(@NotNull String id) {
        this.id = id;
    }
}
