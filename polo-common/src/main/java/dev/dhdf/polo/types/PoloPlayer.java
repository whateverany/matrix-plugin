package dev.dhdf.polo.types;

import org.json.JSONObject;

import java.util.UUID;

public class PoloPlayer {
    public final String name;
    public final String uuid;

    public PoloPlayer(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid.toString().replace("-", "");
    }

    public JSONObject toJSON() {
        return new JSONObject()
                .put("name", this.name)
                .put("uuid", this.uuid);
    }
}
