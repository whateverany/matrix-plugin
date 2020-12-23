package dev.dhdf.polo.types;

import org.json.JSONObject;

public class MCQuit extends MCEvent {
    public final PoloPlayer player;

    public MCQuit(PoloPlayer player) {
        this.player = player;
    }

    @Override
    public JSONObject toJSON() {
        return new JSONObject()
            .put("player", player.toJSON());
    }
}
