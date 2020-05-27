package dev.dhdf.polo.webclient.types;

import org.json.JSONStringer;

public class MCMessage {
    public final PoloPlayer player;
    public final String body;

    public MCMessage(PoloPlayer player, String message) {
        this.player = player;
        this.body = message;
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
                .key("body")
                .value(this.body)
                .endObject()
                .toString();
    }
}
