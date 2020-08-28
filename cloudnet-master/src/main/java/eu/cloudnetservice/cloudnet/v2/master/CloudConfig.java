package eu.cloudnetservice.cloudnet.v2.master;

import com.google.gson.reflect.TypeToken;
import eu.cloudnetservice.cloudnet.v2.lib.ConnectableAddress;
import eu.cloudnetservice.cloudnet.v2.lib.NetworkUtils;
import eu.cloudnetservice.cloudnet.v2.lib.server.ProxyGroup;
import eu.cloudnetservice.cloudnet.v2.lib.server.ServerGroup;
import eu.cloudnetservice.cloudnet.v2.lib.user.BasicUser;
import eu.cloudnetservice.cloudnet.v2.lib.user.User;
import eu.cloudnetservice.cloudnet.v2.lib.utility.document.Document;
import eu.cloudnetservice.cloudnet.v2.master.network.components.Wrapper;
import eu.cloudnetservice.cloudnet.v2.master.network.components.WrapperMeta;
import eu.cloudnetservice.cloudnet.v2.master.util.defaults.BungeeGroup;
import eu.cloudnetservice.cloudnet.v2.master.util.defaults.LobbyGroup;
import eu.cloudnetservice.cloudnet.v2.web.server.util.WebServerConfig;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by Tareko on 16.09.2017.
 */
public class CloudConfig {

    private static final ConfigurationProvider CONFIGURATION_PROVIDER = ConfigurationProvider.getProvider(YamlConfiguration.class);
    private static final Type WRAPPER_META_TYPE = TypeToken.getParameterized(List.class, WrapperMeta.class).getType();
    private static final Type COLLECTION_PROXY_GROUP_TYPE = TypeToken.getParameterized(Collection.class, ProxyGroup.TYPE).getType();
    private static final Type COLLECTION_USER_TYPE = TypeToken.getParameterized(Collection.class, User.class).getType();
    private static final Type COLLECTION_SERVERGROUP_TYPE = TypeToken.getParameterized(Collection.class, ServerGroup.TYPE).getType();

    private static final Path[] MASTER_PATHS = {
        Paths.get("local", "servers"),
        Paths.get("local", "templates"),
        Paths.get("local", "plugins"),
        Paths.get("local", "cache"),
        Paths.get("groups")
    };

    private final Path configPath = Paths.get("config.yml");
    private final Path servicePath = Paths.get("services.json");
    private final Path usersPath = Paths.get("users.json");

    private Collection<ConnectableAddress> addresses;

    private boolean autoUpdate;

    private boolean notifyService;

    private boolean showDescription;
    private boolean showMenu;

    private String formatSplitter, wrapperKey;

    private WebServerConfig webServerConfig;

    private List<WrapperMeta> wrappers;

    private Configuration config;

    private Document serviceDocument, userDocument;

    private List<String> disabledModules;

    private List<String> hasteServer;
    private boolean showGroup;
    private boolean autoList;
    private boolean elof;
    private String color = "§3";
    private String groupColor = "§8";
    private boolean aliases;

    public CloudConfig() {

        for (Path path : MASTER_PATHS) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Folder path " + path.toAbsolutePath().toString() + " could not be created", e);
            }
        }

        NetworkUtils.writeWrapperKey();

        defaultInit();
        defaultInitDoc();
        defaultInitUsers();
        load();
    }

    private void defaultInit() {
        if (Files.exists(configPath)) {
            return;
        }

        String hostName = NetworkUtils.getHostName();

        Configuration configuration = new Configuration();

        configuration.set("general.auto-update", false);
        configuration.set("general.server-name-splitter", "-");
        configuration.set("general.notify-service", true);
        configuration.set("general.disabled-modules", new ArrayList<>());

        configuration.set("general.haste.server", Arrays.asList("https://hastebin.com",
                                                                "https://hasteb.in",
                                                                "https://just-paste.it"));

        configuration.set("server.hostaddress", hostName);
        configuration.set("server.ports", Collections.singletonList(1410));
        configuration.set("server.webservice.hostaddress", hostName);
        configuration.set("server.webservice.port", 1420);

        configuration.set("cloudnet-statistics.enabled", true);
        configuration.set("cloudnet-statistics.uuid", UUID.randomUUID().toString());

        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(configPath), StandardCharsets.UTF_8)) {
            CONFIGURATION_PROVIDER.save(configuration, outputStreamWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void defaultInitDoc() {
        if (Files.exists(servicePath)) {
            return;
        }

        new Document("wrapper", new ArrayList<>())
            .append("proxyGroups", Collections.singletonList(new BungeeGroup())).saveAsConfig(servicePath);

        new Document("group", new LobbyGroup()).saveAsConfig(Paths.get("groups/Lobby.json"));
    }

    private void defaultInitUsers() {
        if (Files.exists(usersPath)) {
            return;
        }

        String password = NetworkUtils.randomString(32);
        System.out.printf("\"admin\" Password: %s%n", password);
        System.out.println(NetworkUtils.SPACE_STRING);
        new Document("users",
                     Collections.singletonList(
                         new BasicUser("admin", password, Collections.singletonList("*"))))
            .saveAsConfig(usersPath);
    }

    public CloudConfig load() {

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Configuration configuration = CONFIGURATION_PROVIDER.load(reader);
            this.config = configuration;

            String host = configuration.getString("server.hostaddress");

            Collection<ConnectableAddress> addresses = new ArrayList<>();
            for (int value : configuration.getIntList("server.ports")) {
                addresses.add(new ConnectableAddress(host, value));
            }
            this.addresses = addresses;

            this.wrapperKey = NetworkUtils.readWrapperKey();
            this.autoUpdate = configuration.getBoolean("general.auto-update");
            this.notifyService = configuration.getBoolean("general.notify-service");
            this.webServerConfig = new WebServerConfig(true,
                                                       configuration.getString("server.webservice.hostaddress"),
                                                       configuration.getInt("server.webservice.port"));
            this.formatSplitter = configuration.getString("general.server-name-splitter");

            if (!configuration.getSection("general").contains("disabled-modules")) {
                configuration.set("general.disabled-modules", new ArrayList<>());

                try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                    CONFIGURATION_PROVIDER.save(configuration, writer);
                }
            }
            if (!configuration.getSection("general").contains("haste")) {
                configuration.set("general.haste.server", Arrays.asList("https://hastebin.com",
                                                                        "https://hasteb.in",
                                                                        "https://just-paste.it"));

                try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                    CONFIGURATION_PROVIDER.save(configuration, writer);
                }
            }
            if (!configuration.contains("console")) {
                configuration.set("console.showDescription", false);
                configuration.set("console.showMenu", true);
                configuration.set("console.showGroup", false);
                configuration.set("console.autoList", true);
                configuration.set("console.elof", false);
                configuration.set("console.aliases", false);
                configuration.set("console.color", "§3");
                configuration.set("console.groupColor", "§8");

                try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                    CONFIGURATION_PROVIDER.save(configuration, writer);
                }
            }
            this.hasteServer = configuration.getStringList("general.haste.server");
            this.showDescription = configuration.getBoolean("console.showDescription");
            this.showMenu = configuration.getBoolean("console.showMenu");
            this.showGroup = configuration.getBoolean("console.showGroup");
            this.autoList = configuration.getBoolean("console.autoList");
            this.aliases = configuration.getBoolean("console.aliases");
            this.elof = configuration.getBoolean("console.elof");
            this.color = configuration.getString("console.color");
            this.groupColor = configuration.getString("console.groupColor");

            this.disabledModules = configuration.getStringList("general.disabled-modules");
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.serviceDocument = Document.loadDocument(servicePath);

        this.wrappers = this.serviceDocument.getObject("wrapper", WRAPPER_META_TYPE);

        this.userDocument = Document.loadDocument(usersPath);

        return this;
    }

    public void createWrapper(WrapperMeta wrapperMeta) {
        Collection<WrapperMeta> wrapperMetas = this.serviceDocument.getObject("wrapper", WRAPPER_META_TYPE);
        wrapperMetas.removeIf(meta -> meta.getId().equals(wrapperMeta.getId()));
        wrapperMetas.add(wrapperMeta);
        this.serviceDocument.append("wrapper", wrapperMetas).saveAsConfig(servicePath);
        CloudNet.getInstance().getWrappers().put(wrapperMeta.getId(), new Wrapper(wrapperMeta));
    }

    public void deleteWrapper(WrapperMeta wrapperMeta) {
        this.serviceDocument.append("wrapper", this.wrappers = this.deleteWrapper0(wrapperMeta)).saveAsConfig(this.servicePath);
        CloudNet.getInstance().getWrappers().remove(wrapperMeta.getId());
    }

    private List<WrapperMeta> deleteWrapper0(WrapperMeta wrapperMeta) {
        List<WrapperMeta> wrapperMetas = this.serviceDocument.getObject("wrapper", WRAPPER_META_TYPE);
        wrapperMetas.removeIf(meta -> meta.getId().equals(wrapperMeta.getId()));
        return wrapperMetas;
    }

    public Collection<User> getUsers() {
        if (this.userDocument == null) {
            return null;
        }
        return userDocument.getObject("users", COLLECTION_USER_TYPE);
    }

    public CloudConfig save(Collection<User> users) {
        if (userDocument != null) {
            userDocument.append("users", users).saveAsConfig(usersPath);
        }
        return this;
    }

    public void createGroup(ProxyGroup proxyGroup) {
        Collection<ProxyGroup> groups = this.serviceDocument.getObject("proxyGroups", COLLECTION_PROXY_GROUP_TYPE);
        groups.removeIf(value -> value.getName().equals(proxyGroup.getName()));
        groups.add(proxyGroup);
        this.serviceDocument.append("proxyGroups", groups).saveAsConfig(servicePath);
    }

    public void deleteGroup(ServerGroup serverGroup) {
        try {
            Files.deleteIfExists(Paths.get("groups", serverGroup.getName() + ".json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteGroup(ProxyGroup proxyGroup) {
        Collection<ProxyGroup> groups = this.serviceDocument.getObject("proxyGroups", COLLECTION_PROXY_GROUP_TYPE);
        groups.removeIf(value -> value.getName().equals(proxyGroup.getName()));
        this.serviceDocument.append("proxyGroups", groups).saveAsConfig(servicePath);
    }

    public Map<String, ServerGroup> getServerGroups() {
        Map<String, ServerGroup> groups = new ConcurrentHashMap<>();

        if (serviceDocument.contains("serverGroups")) {

            Collection<ServerGroup> serverGroups = serviceDocument.getObject("serverGroups", COLLECTION_SERVERGROUP_TYPE);

            for (ServerGroup serverGroup : serverGroups) {
                createGroup(serverGroup);
            }

            serviceDocument.remove("serverGroups");
            serviceDocument.saveAsConfig(servicePath);
        }

        File groupsDirectory = new File("groups");

        if (groupsDirectory.isDirectory()) {
            File[] files = groupsDirectory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".json")) {
                        try {
                            Document entry = Document.loadDocument(file);
                            ServerGroup serverGroup = entry.getObject("group", ServerGroup.TYPE);
                            groups.put(serverGroup.getName(), serverGroup);
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                            System.out.println("Cannot load servergroup file [" + file.getName() + ']');
                        }
                    }
                }
            }
        }

        return groups;
    }

    public void createGroup(ServerGroup serverGroup) {
        new Document("group", serverGroup)
            .saveAsConfig(Paths.get("groups", serverGroup.getName() + ".json"));
    }

    public Map<String, ProxyGroup> getProxyGroups() {
        Collection<ProxyGroup> proxyGroups = serviceDocument.getObject("proxyGroups", COLLECTION_PROXY_GROUP_TYPE);

        Map<String, ProxyGroup> proxyGroupMap = new HashMap<>();
        for (ProxyGroup proxyGroup : proxyGroups) {
            proxyGroupMap.put(proxyGroup.getName(), proxyGroup);
        }
        return proxyGroupMap;
    }


    public Path getConfigPath() {
        return this.configPath;
    }

    public Path getServicePath() {
        return this.servicePath;
    }

    public Path getUsersPath() {
        return this.usersPath;
    }

    public Collection<ConnectableAddress> getAddresses() {
        return this.addresses;
    }

    public boolean isAutoUpdate() {
        return this.autoUpdate;
    }

    public boolean isNotifyService() {
        return this.notifyService;
    }

    public String getFormatSplitter() {
        return this.formatSplitter;
    }

    public String getWrapperKey() {
        return this.wrapperKey;
    }

    public WebServerConfig getWebServerConfig() {
        return this.webServerConfig;
    }

    public List<WrapperMeta> getWrappers() {
        return this.wrappers;
    }

    public Configuration getConfig() {
        return this.config;
    }

    public Document getServiceDocument() {
        return this.serviceDocument;
    }

    public Document getUserDocument() {
        return this.userDocument;
    }

    public List<String> getDisabledModules() {
        return this.disabledModules;
    }

    public List<String> getHasteServer() {
        return this.hasteServer;
    }

    public boolean isShowDescription() {
        return showDescription;
    }

    public void setShowDescription(final boolean showDescription) {
        this.showDescription = showDescription;
        this.config.set("console.showDescription", showDescription);

        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            CONFIGURATION_PROVIDER.save(this.config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean isShowMenu() {
        return showMenu;
    }

    public void setShowMenu(final boolean showMenu) {
        this.showMenu = showMenu;
        this.config.set("console.showMenu", showMenu);
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            CONFIGURATION_PROVIDER.save(this.config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isShowGroup() {
        return showGroup;
    }

    public void setShowGroup(final boolean showGroup) {
        this.showGroup = showGroup;
        this.config.set("console.showGroup", showGroup);
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            CONFIGURATION_PROVIDER.save(this.config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isAutoList() {
        return autoList;
    }

    public void setAutoList(final boolean autoList) {
        this.autoList = autoList;
        this.config.set("console.autoList", autoList);
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            CONFIGURATION_PROVIDER.save(this.config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isElof() {
        return elof;
    }

    public void setElof(final boolean elof) {
        this.elof = elof;
        this.config.set("console.elof", elof);
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            CONFIGURATION_PROVIDER.save(this.config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getColor() {
        return color;
    }

    public String getGroupColor() {
        return groupColor;
    }

    public void setColor(final String color) {
        this.color = color;
        this.config.set("console.color", color);
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            CONFIGURATION_PROVIDER.save(this.config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setGroupColor(final String groupColor) {
        this.groupColor = groupColor;
        this.config.set("console.groupColor", groupColor);
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            CONFIGURATION_PROVIDER.save(this.config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isAliases() {
        return aliases;
    }

    public void setAliases(final boolean aliases) {
        this.aliases = aliases;
        this.config.set("console.aliases", aliases);
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            CONFIGURATION_PROVIDER.save(this.config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
