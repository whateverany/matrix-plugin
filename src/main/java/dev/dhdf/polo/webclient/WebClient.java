package dev.dhdf.polo.webclient;

import dev.dhdf.polo.webclient.types.MCMessage;
import dev.dhdf.polo.webclient.types.MxMessage;
import dev.dhdf.polo.webclient.types.PoloPlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;


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
    private final Server server;

    public WebClient(Server server, Config config) {
        this.address = config.address;
        this.port = config.port;
        this.token = config.token;
        this.server = server;
    }

    /**
     * Send new chat messages to Marco
     *
     * @param mcPlayer Player object representing a Minecraft player, it
     *                 must be parsed before sent to Marco
     * @param context  The body of the message
     */
    public void postChat(Player mcPlayer, String context) {
        PoloPlayer player = new PoloPlayer(mcPlayer);
        MCMessage message = new MCMessage(player, context);
        String body = message.toString();

        this.doRequest(
                "POST",
                "/chat",
                body
        );
    }

    /**
     * Get new messages from Marco and the Matrix room
     */
    public void getChat() {
        JSONObject chatResponse = this.doRequest(
                "GET",
                "/chat",
                null
        );
        JSONArray messages = chatResponse.getJSONArray("chat");

        // Send all the new messages to the minecraft chat
        for (int i = 0; i < messages.length(); ++i) {
            JSONObject message = messages.getJSONObject(i);
            MxMessage parsedMessage = new MxMessage(message);
            onRoomMessage(parsedMessage);
        }
    }

    public void onRoomMessage(MxMessage message) {
        this.server.broadcastMessage(message.body);
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
                    null
            );

            return check.getString("status").equals("OK");

        } catch (NullPointerException err) {
            return false;
        }
    }

    public JSONObject doRequest(String method, String endpoint, String body) {
        Logger logger = server.getLogger();
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

            InputStream stream = connection.getErrorStream();
            int resCode = connection.getResponseCode();

            if (resCode != 404) {
                if (stream == null)
                    stream = connection.getInputStream();

                if (stream.toString().length() > 0) {
                    JSONTokener parsing = new JSONTokener(stream);
                    JSONObject parsed = new JSONObject(parsing);

                    if (resCode != 200) {
                        logger.warning("An error has occurred");
                        logger.warning(parsed.getString("error"));
                        logger.warning(parsed.getString("message"));
                    }

                    return parsed;
                } else {
                    return null;
                }
            } else {
                logger.severe("An invalid endpoint was called for.");
                return null;
            }
        } catch (IOException | JSONException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }
}
