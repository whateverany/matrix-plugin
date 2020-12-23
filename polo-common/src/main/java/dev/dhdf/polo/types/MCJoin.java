package dev.dhdf.polo.types;

import org.json.JSONObject;

public class MCJoin extends MCEvent {
    public final PoloPlayer player;

    public MCJoin(PoloPlayer player) {
        this.player = player;
    }

    @Override
    public JSONObject toJSON() {
        return new JSONObject()
            .put("player", player.toJSON());
    }
}
