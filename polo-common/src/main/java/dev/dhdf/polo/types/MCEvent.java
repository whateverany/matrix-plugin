package dev.dhdf.polo.types;

import org.json.JSONObject;

public abstract class MCEvent {
    public abstract JSONObject toJSON();

    public String toString() {
        return toJSON().toString();
    }
}
