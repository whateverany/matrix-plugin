package dev.dhdf.polo.types;

import org.json.JSONStringer;

public class MCMessage {
    public final PoloPlayer player;
    public final String message;

    public MCMessage(PoloPlayer player, String message) {
        this.player = player;
        this.message = message;
    }

    public String toString() {
        return new JSONStringer()
                .object()
                .key("player")
                .object()
                .key("name")
                .value(this.player.name)
                .key("uuid")
                .value(this.player.uuid)
                .endObject()
                .key("message")
                .value(this.message)
                .endObject()
                .toString();
    }
}
