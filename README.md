# Matrix Plugin
Built for: [Matrix Minecraft Appservice (0.9.7)](https://github.com/dhghf/matrix-appservice-minecraft)

## Setup
 1. Run `gradle build`

### Setup Bukkit / Spigot
 1. Copy `polo-bukkit/build/libs/polo-bukkit-1.0.0-all.jar`
 2. Send it to your Minecraft server plugins directory

While in your server's directory:
 1. Run your server to generate the config.yaml
 2. Modify plugins/polo/config.yml
 3. Set address to where Matrix Minecraft Appservice is running
 4. Set port to the port of Matrix Minecraft Appservice
 5. Set token provided by `@_mc_bot` by doing 
 `!minecraft bridge <room ID>`
 6. You're now ready (restart or run /reload)

