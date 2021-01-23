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

    /**
     * Read a UUID from a string that has had the dashes removed.
     *
     * Read a UUID from the appservice, which will have originated from
     * PoloPlayer, which removes the dashes. We have to add them back in before
     * converting to a UUID object.
     *
     * @param uuid UUID string without dashes.
     * @return UUID.
     */
    public static UUID uuidFromString(String uuid) {
        uuid = uuid.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5");
        return java.util.UUID.fromString(uuid);
    }

    public JSONObject toJSON() {
        return new JSONObject()
                .put("name", this.name)
                .put("uuid", this.uuid);
    }
}
