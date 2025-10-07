package appland.telemetry.appinsights;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
enum Tag {
    OsVersion("ai.device.osVersion"),
    UserId("ai.user.id"),
    SessionId("ai.session.id");

    private final @NotNull String id;

    Tag(@NotNull String id) {
        this.id = id;
    }
}
