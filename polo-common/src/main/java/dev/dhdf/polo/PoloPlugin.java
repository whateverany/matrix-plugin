package dev.dhdf.polo;

import java.util.UUID;

public interface PoloPlugin {
    /**
     * Broadcast a Matrix message to the server.
     * @param message The message to broadcast
     */
    public void broadcastMessage(String message, Object json);

    /**
     * Kick a player from the server.
     * @param uuid Player UUID
     * @param reason The reason for the kick
     * @param source Where the kick came from
     */
    public void kickPlayer(UUID uuid, String reason, String source);

    /**
     * Ban a player from the server.
     * @param uuid Player UUID
     * @param reason The reason for the ban
     * @param source Where the ban came from
     */
    public void banPlayer(UUID uuid, String reason, String source);

    /**
     * Unban a player from the server.
     * @param uuid Player UUID
     * @param source Where the unban came from
     */
    public void unbanPlayer(UUID uuid, String source);

    /**
     * Execute a task asynchronously using the plugin's scheduler.
     * @param task The task to execute asynchronously
     */
    public void executeAsync(Runnable task);
}
