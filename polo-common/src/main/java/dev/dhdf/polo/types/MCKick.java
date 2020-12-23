package dev.dhdf.polo.types;

import org.json.JSONObject;

public class MCKick extends MCEvent {
    public final PoloPlayer player;
    public final String reason;

    public MCKick(PoloPlayer player, String reason) {
        this.player = player;
        this.reason = reason;
    }

    @Override
    public JSONObject toJSON() {
        return new JSONObject()
            .put("player", player.toJSON())
            .put("reason", this.reason);
    }
}
