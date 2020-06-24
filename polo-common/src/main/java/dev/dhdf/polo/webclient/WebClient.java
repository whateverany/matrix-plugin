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

    private final Logger logger = LoggerFactory.getLogger(WebClient.class);
    private final PoloPlugin plugin;

    public WebClient(PoloPlugin plugin, Config config) {
        this.address = config.address;
        this.port = config.port;
        this.token = config.token;
        this.plugin = plugin;
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
    public void getChat() {
        JSONObject chatResponse = this.doRequest(
                "GET",
                "/chat",
                null,
                true
        );
        if (chatResponse == null)
            return;
        JSONArray messages = chatResponse.getJSONArray("chat");

        // Send all the new messages to the minecraft chat
        for (int i = 0; i < messages.length(); ++i) {
            String message = messages.getString(i);
            onRoomMessage(message);
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
