package dev.dhdf.polo.bukkit;

import dev.dhdf.polo.PoloPlugin;
import dev.dhdf.polo.util.Sync;
import dev.dhdf.polo.webclient.Config;
import dev.dhdf.polo.webclient.WebClient;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.BanList;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.UUID;


/**
 * This starts the plugin
 */
public class Main extends JavaPlugin implements PoloPlugin {
    @Override
    public void onEnable() {
        // Save the default config if it isn't already there
        this.saveDefaultConfig();

        // First read from the config
        FileConfiguration pluginConfig = this.getConfig();
        Logger logger = getLogger();
        boolean isValid = checkIntegrity(pluginConfig);

        logger.info("Started Polo plugin");

        if (!isValid)
            logger.severe("Incomplete config.yml, please modify");

        Config config = new Config(
                pluginConfig.getString("address"),
                pluginConfig.getInt("port"),
                pluginConfig.getString("token"),
                pluginConfig.getBoolean("relay-mc-membership"),
                pluginConfig.getBoolean("relay-mx-kicks"),
                pluginConfig.getBoolean("relay-mx-bans")
        );

        // Start up the web client
        WebClient webClient = new WebClient(
                this,
                config
        );

        // Make the WebClient log through our PluginLogger
        Logger.getLogger(WebClient.class.getName()).setParent(getLogger());

        // Start up the Minecraft event listener
        MCListener mcListener = new MCListener(webClient);

        getServer().getPluginManager().registerEvents(mcListener, this);

        logger.info("Started webclient and chat listeners");

        // See if the address and port are pointing to Marco
        boolean vibeCheck = webClient.vibeCheck();

        if (vibeCheck) {
            logger.finer("Started bridge");
            Sync sync = new Sync(webClient);
            getServer().getScheduler().runTaskTimerAsynchronously(this, sync, 0, 5*20);
        } else {
            logger.severe("Couldn't properly connect to marco is the address and port set properly?");
        }
    }

    @Override
    public void onDisable() {
    }


    /**
     * This checks the integrity of the config.yaml
     *
     * @param config The config to review
     * @return {boolean}
     */
    private boolean checkIntegrity(FileConfiguration config) {
        String address = config.getString("address");
        String token = config.getString("token");
        int port = config.getInt("port");

        if (address == null || address.isEmpty())
            return false;
        else if (token == null || token.isEmpty())
            return false;
        else
            return (port != 0);
    }

    @Override
    public void broadcastMessage(String message) {
        this.getServer().broadcastMessage(message);
    }

    @Override
    public void kickPlayer(UUID uuid, String reason, String source) {
        Player player = this.getServer().getPlayer(uuid);
        if (player == null) {
            Logger logger = getLogger();
            logger.warning("No player to kick with UUID " + uuid.toString());
            return;
        }
        getServer().getScheduler().runTask(this, new Runnable() {
            public void run() {
                player.kickPlayer(reason);
                getServer().broadcast(source + " kicked " + player.getName() + " for " + reason, "matrix.kick.notify");
            }
        });
    }

    @Override
    public void banPlayer(UUID uuid, String reason, String source) {
        Logger logger = getLogger();
        OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(uuid);
        if (offlinePlayer == null) {
            logger.warning("No player to ban with UUID " + uuid.toString());
            return;
        }
        BanList banlist = getServer().getBanList(BanList.Type.NAME);
        if (banlist == null) {
            logger.warning("No banlist found");
            return;
        }
        banlist.addBan(offlinePlayer.getName(), reason, null, source);
        getServer().broadcast(source + " banned " + offlinePlayer.getName() + " for " + reason, "matrix.ban.notify");

        Player player = getServer().getPlayer(uuid);
        if (player != null) {
            // If online, kick now
            getServer().getScheduler().runTask(this, new Runnable() {
                public void run() {
                    player.kickPlayer("You have been banned: " + reason);
                }
            });
        }
    }

    @Override
    public void unbanPlayer(UUID uuid, String source) {
        Logger logger = getLogger();
        OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(uuid);
        if (offlinePlayer == null) {
            logger.warning("No player to unban with UUID " + uuid.toString());
            return;
        }
        BanList banlist = getServer().getBanList(BanList.Type.NAME);
        if (banlist == null) {
            logger.warning("No banlist found");
            return;
        }
        banlist.pardon(offlinePlayer.getName());
        getServer().broadcast(source + " unbanned " + offlinePlayer.getName(), "matrix.unban.notify");
    }

    @Override
    public void executeAsync(Runnable task) {
        this.getServer().getScheduler().runTaskAsynchronously(this, task);
    }
}
