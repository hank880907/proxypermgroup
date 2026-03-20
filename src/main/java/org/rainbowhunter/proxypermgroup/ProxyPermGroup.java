package org.rainbowhunter.proxypermgroup;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.matcher.NodeMatcher;
import net.luckperms.api.node.types.InheritanceNode;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Plugin(id = "proxypermgroup",
        name = "ProxyPermGroup",
        version = BuildConstants.VERSION, 
        dependencies = {
            @Dependency(id = "luckperms")
        },
        authors = {"Hank Wu"}
)
public class ProxyPermGroup {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private ConfigurationNode config;
    private String permGroup;

    @Inject
    public ProxyPermGroup(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        loadConfig();
        permGroup = config.node("permission_group").getString("velocity");
        logger.info("ProxyPermGroup loaded with group " + permGroup);
        LuckPerms luckperms = LuckPermsProvider.get();
        GroupManager groupManager = luckperms.getGroupManager();
        UserManager userManager = luckperms.getUserManager();
        Group group = groupManager.getGroup(permGroup);
        if (group == null) {
            logger.info("Creating Permission Group " + permGroup);
             groupManager.createAndLoadGroup(permGroup).thenRun(() -> {
                logger.info(permGroup + " created!");
            });
        } else {
            NodeMatcher<InheritanceNode> matcher = NodeMatcher.key(InheritanceNode.builder(permGroup).build());
            userManager.searchAll(matcher).thenCompose(results -> {
                InheritanceNode node = InheritanceNode.builder(permGroup).build();
                List<CompletableFuture<Void>> saveOperations = new ArrayList<>();
                for (UUID uuid : results.keySet()) {
                    CompletableFuture<Void> modifyTask = userManager.modifyUser(uuid, user -> {
                        user.data().remove(node);
                    });
                    saveOperations.add(modifyTask);
                }
                return CompletableFuture.allOf(saveOperations.toArray(new CompletableFuture[0]));
            });
        }
    }

    @Subscribe
    public void onLoginEvent(LoginEvent event) {
        Player player = event.getPlayer();
        assignPlayerToGroup(player, permGroup);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        removePlayerFromGroup(player, permGroup);
    }

    private void loadConfig() {
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Failed to create the plugin data directory", e);
            }
        }
        File file = new File(dataDirectory.toFile(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                logger.error("Failed to create default config file", e);
            }
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .build();

        try {
            config = loader.load();
        } catch (IOException e) {
            logger.error("An error occurred while loading this configuration", e);
        }
    }

    private void assignPlayerToGroup(Player player, String groupName) {
        LuckPerms luckperms = LuckPermsProvider.get();
        GroupManager groupManager = luckperms.getGroupManager();
        UserManager userManager = luckperms.getUserManager();
        InheritanceNode node = InheritanceNode.builder(groupName).build();

        userManager.modifyUser(player.getUniqueId(), user -> {
            user.data().add(node);
        });
    }

    private void removePlayerFromGroup(Player player, String groupName) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        UserManager userManager = luckPerms.getUserManager();
        InheritanceNode node = InheritanceNode.builder(groupName).build();
        userManager.modifyUser(player.getUniqueId(), user -> {
            user.data().remove(node);
        });
    }
}
