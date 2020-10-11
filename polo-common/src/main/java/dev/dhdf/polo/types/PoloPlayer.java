package dev.dhdf.polo.types;

import org.json.JSONObject;

import java.util.UUID;

public class PoloPlayer {
    public final String name;
    public final String uuid;
    public final String texture;

    public PoloPlayer(String name, UUID uuid, String texture) {
        this.name = name;
        this.uuid = uuid.toString().replace("-", "");
        this.texture = texture;
    }

    public JSONObject toJSON() {
        return new JSONObject()
                .put("name", this.name)
                .put("uuid", this.uuid)
                .put("texture", this.texture);
    }
}
