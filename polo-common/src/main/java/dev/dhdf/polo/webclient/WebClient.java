package dev.dhdf.polo.webclient;

import dev.dhdf.polo.PoloPlugin;
import dev.dhdf.polo.types.MCMessage;
import dev.dhdf.polo.types.PoloPlayer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;


/**
 * This is Polo. It interacts with Marco to establish a bridge with a room.
 * Polo periodically sends GET requests to see all the new messages in the
 * Matrix room. It also sends POST requests to Marco including all the new
 * events that occurred (which is only chat messages at the moment)
 */
public class WebClient {
    private final String address;
    private final int port;
    private final String token;
    private final boolean relay_mc_membership;
    private final boolean relay_mx_kicks;
    private final boolean relay_mx_bans;

    private final Logger logger = LoggerFactory.getLogger(WebClient.class);
    private final PoloPlugin plugin;

    public WebClient(PoloPlugin plugin, Config config) {
        this.address = config.address;
        this.port = config.port;
        this.token = config.token;
        this.relay_mc_membership = config.relay_mc_membership;
        this.relay_mx_kicks = config.relay_mx_kicks;
        this.relay_mx_bans = config.relay_mx_bans;
        this.plugin = plugin;
    }

    public void postJoin(PoloPlayer player, String context) {
        if (!relay_mc_membership)
            return;

        MCMessage message = new MCMessage(player, context);
        String body = message.toString();

        // Run communication outside the server thread
        plugin.executeAsync(() ->
                this.doRequest(
                        "POST",
                        "/chat/join",
                        body,
                        false
                )
        );
    }

    public void postQuit(PoloPlayer player, String context) {
        if (!relay_mc_membership)
            return;

        MCMessage message = new MCMessage(player, context);
        String body = message.toString();

        // Run communication outside the server thread
        plugin.executeAsync(() ->
                this.doRequest(
                        "POST",
                        "/chat/quit",
                        body,
                        false
                )
        );
    }

    public void postKick(PoloPlayer player, String reason) {
        if (!relay_mc_membership)
            return;

        MCMessage message = new MCMessage(player, reason);
        String body = message.toString();

        // Run communication outside the server thread
        plugin.executeAsync(() ->
                this.doRequest(
                        "POST",
                        "/chat/kick",
                        body,
                        false
                )
        );
    }

    /**
     * Send new chat messages to Marco
     *
     * @param player Player object representing a Minecraft player, it
     *                 must be parsed before sent to Marco
     * @param context  The body of the message
     */
    public void postChat(PoloPlayer player, String context) {
        MCMessage message = new MCMessage(player, context);
        String body = message.toString();

        // Run communication outside the server thread
        plugin.executeAsync(() ->
                this.doRequest(
                        "POST",
                        "/chat",
                        body,
                        false
                )
        );
    }

    /**
     * Get new messages from Marco and the Matrix room
     */
    public boolean getChat() {
        JSONObject chatResponse = this.doRequest(
                "GET",
                "/chat",
                null,
                true
        );
        if (chatResponse == null)
            return false;
        JSONArray messages = chatResponse.getJSONArray("chat");

        // Send all the new messages to the minecraft chat
        for (int i = 0; i < messages.length(); ++i) {
            String message = messages.getString(i);
            onRoomMessage(message);
        }

        return true;
    }

    /**
     * Get new events from Marco and the Matrix room
     */
    public boolean getEvents() {
        JSONObject eventsResponse = this.doRequest(
                "GET",
                "/chat/events",
                null,
                true
        );
        if (eventsResponse == null)
            return false;
        JSONArray events = eventsResponse.getJSONArray("events");

        for (int i = 0; i < events.length(); ++i) {
            JSONObject event = events.getJSONObject(i);

            JSONObject sender = event.getJSONObject("sender");
            String senderDisplayName = sender.getString("displayName");
            String type = event.getString("type");
            String reason = null;
            switch (type) {
                case "dev.dhdf.mx.message.text":
                case "dev.dhdf.mx.message.emote":
                case "dev.dhdf.mx.message.announce":
                    String body = event.getString("body");
                    switch (type) {
                        case "dev.dhdf.mx.message.text":
                            body = "<" + senderDisplayName + "> " + body;
                            break;
                        case "dev.dhdf.mx.message.emote":
                            body = " * <" + senderDisplayName + "> " + body;
                            break;
                        case "dev.dhdf.mx.message.announce":
                            body = "[Server]" + body;
                            break;
                    }
                    onRoomMessage(body);
                    break;

                case "dev.dhdf.mx.player.kick":
                case "dev.dhdf.mx.player.ban":
                    // reason is optional
                    if (event.has("reason"))
                        reason = event.getString("reason");
                    // Fall-through
                case "dev.dhdf.mx.player.unban":
                    JSONObject player = event.getJSONObject("player");
                    String player_uuid_raw = player.getString("uuid");
                    // Re-insert the dashes into the UUID (see PoloPlayer constructor)
                    String player_uuid_str = player_uuid_raw.replaceFirst(
                            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                            "$1-$2-$3-$4-$5");
                    UUID player_uuid = java.util.UUID.fromString(player_uuid_str);
                    switch (type) {
                        case "dev.dhdf.mx.player.kick":
                            onPlayerKick(player_uuid, reason, senderDisplayName);
                            break;
                        case "dev.dhdf.mx.player.ban":
                            onPlayerBan(player_uuid, reason, senderDisplayName);
                            break;
                        case "dev.dhdf.mx.player.unban":
                            onPlayerUnban(player_uuid, senderDisplayName);
                            break;
                    }
                    break;

                default:
                    logger.warn("Unknown matrix event type '"+type+"' ignored");
                    break;
            }
        }

        return true;
    }

    public void onRoomMessage(String message) {
        this.plugin.broadcastMessage(message);
    }

    public void onPlayerKick(UUID uuid, String reason, String source) {
        if (relay_mx_kicks) {
            if (reason == null)
                reason = "Kicked from server.";
            this.plugin.kickPlayer(uuid, reason, source);
        }
    }

    public void onPlayerBan(UUID uuid, String reason, String source) {
        if (relay_mx_bans) {
            if (reason == null)
                reason = "Banned.";
            this.plugin.banPlayer(uuid, reason, source);
        }
    }

    public void onPlayerUnban(UUID uuid, String source) {
        if (relay_mx_bans)
            this.plugin.unbanPlayer(uuid, source);
    }

    /**
     * See if we're connecting to Marco properly / the token we have is
     * valid
     *
     * @return boolean
     */
    public boolean vibeCheck() {
        try {
            JSONObject check = this.doRequest(
                    "GET",
                    "/vibeCheck",
                    null,
                    true
            );

            return check.getString("status").equals("OK");

        } catch (NullPointerException err) {
            return false;
        }
    }

    public JSONObject doRequest(String method, String endpoint, String body, Boolean expectJSON) {
        try {
            URL url = new URL(
                    "http://" + address + ":" + port + endpoint
            );

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Bearer " + this.token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Marco Spigot Plugin");

            if (!method.equals("GET") && body != null) {
                connection.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(body);
                writer.flush();
                writer.close();
            }

            int resCode = connection.getResponseCode();

            if (resCode != 404) {
                InputStream stream = null;
                if (resCode != 200) {
                    logger.warn("An error has occurred: " + resCode);
                    stream = connection.getErrorStream();
                } else if (expectJSON) {
                    stream = connection.getInputStream();
                }

                if (stream != null) {
                    JSONTokener parsing = new JSONTokener(stream);
                    JSONObject parsed = new JSONObject(parsing);

                    if (resCode != 200) {
                        logger.warn(parsed.getString("error"));
                        logger.warn(parsed.getString("message"));
                        return null;
                    }

                    return parsed;
                } else {
                    return null;
                }
            } else {
                logger.error("An invalid endpoint " + endpoint + " was called for.");
                return null;
            }
        } catch (java.net.ConnectException e) {
            logger.warn(e.getMessage());
            return null;
        } catch (IOException | JSONException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }
}
