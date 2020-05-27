package dev.dhdf.polo.webclient.types;

import org.json.JSONObject;
import org.json.JSONStringer;

public class MxMessage {
    public final String author;
    public final String body;
    public final String room;

    public MxMessage(JSONObject rawMessage) {
        this.author = rawMessage.getString("sender");
        this.body = rawMessage.getString("body");
        this.room = rawMessage.getString("room");
    }

    public String toString() {
        return new JSONStringer()
                .key("sender")
                .value(this.author)
                .key("body")
                .value(this.body)
                .key("room")
                .value(this.room)
                .endObject()
                .toString();
    }
}
