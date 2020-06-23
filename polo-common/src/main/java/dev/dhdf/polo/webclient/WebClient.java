package dev.dhdf.polo.webclient;

import dev.dhdf.polo.PoloPlugin;
import dev.dhdf.polo.types.MCMessage;
import dev.dhdf.polo.types.MCJoin;
import dev.dhdf.polo.types.MCQuit;
import dev.dhdf.polo.types.MCKick;
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


/**
 * This is Polo. It interacts with Marco to establish a bridge with a room.
 * Polo periodically sends GET requests to see all the new messages in the
 * Matrix room. It also sends POST requests to Marco including all the new
 * events that occurred (which is only chat messages at the moment)
 */
public class WebClient {
    private final Logger logger = LoggerFactory.getLogger(WebClient.class);
    private final Config config;
    private final PoloPlugin plugin;

    public WebClient(PoloPlugin plugin, Config config) {
        this.config = config;
        this.plugin = plugin;
    }

    /**
     * Send player join event to Marco
     *
     * @param player Player object representing a Minecraft player who has
     *                  joined the server, it must be parsed before sent to
     *                  Marco
     */
    public void postJoin(PoloPlayer player) {
        if (!config.relayMinecraftMembership)
            return;

        MCJoin join = new MCJoin(player);
        String body = join.toString();

        // Run communication outside the server thread
        plugin.executeAsync(() ->
                this.doRequest(
                        "POST",
                        "/player/join",
                        body,
                        false
                )
        );
    }

    /**
     * Send player quit event to Marco
     *
     * @param player Player object representing a Minecraft player who has quit
     *                  the server, it must be parsed before sent to Marco
     */
    public void postQuit(PoloPlayer player) {
        if (!config.relayMinecraftMembership)
            return;

        MCQuit quit = new MCQuit(player);
        String body = quit.toString();

        // Run communication outside the server thread
        plugin.executeAsync(() ->
                this.doRequest(
                        "POST",
                        "/player/quit",
                        body,
                        false
                )
        );
    }

    /**
     * Send player kick event to Marco
     *
     * @param player Player object representing a Minecraft player who has been
     *                  kicked, it must be parsed before sent to Marco
     * @param reason The reason for the player being kicked
     */
    public void postKick(PoloPlayer player, String reason) {
        if (!config.relayMinecraftMembership)
            return;

        MCKick kick = new MCKick(player, reason);
        String body = kick.toString();

        // Run communication outside the server thread
        plugin.executeAsync(() ->
                this.doRequest(
                        "POST",
                        "/player/kick",
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
                "/events",
                null,
                true
        );
        if (eventsResponse == null)
            return false;
        JSONArray events = eventsResponse.getJSONArray("events");

        for (int i = 0; i < events.length(); ++i) {
            JSONObject event = events.getJSONObject(i);
            String type = event.getString("type");
            try {
                handleEvent(type, event);
            } catch (Exception e) {
                logger.warn("Exception while handling matrix event type '{}'", type);
                e.printStackTrace();
            }
        }

        return true;
    }

    private void handleEvent(String type, JSONObject event) {
        JSONObject sender = event.getJSONObject("sender");
        String senderDisplayName = sender.getString("displayName");
        switch (type) {
            case "message.text":
            case "message.emote":
            case "message.announce":
                String body = event.getString("body");
                switch (type) {
                    case "message.text":
                        body = "<" + senderDisplayName + "> " + body;
                        break;
                    case "message.emote":
                        body = " * <" + senderDisplayName + "> " + body;
                        break;
                    case "message.announce":
                        body = "[Server]" + body;
                        break;
                }
                onRoomMessage(body);
                break;

            default:
                logger.warn("Unknown matrix event type '{}' ignored", type);
                break;
        }
    }

    public void onRoomMessage(String message) {
        this.plugin.broadcastMessage(message);
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
                    "http://" + config.address + ":" + config.port + endpoint
            );

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Bearer " + config.token);
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
                logger.error("An invalid endpoint {} was called for.", endpoint);
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
