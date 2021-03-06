package co.chatchain.dc;

import co.chatchain.commons.ChatChainHubConnection;
import co.chatchain.commons.HubModule;
import co.chatchain.commons.configuration.ConfigurationModule;
import co.chatchain.commons.core.entities.Client;
import co.chatchain.commons.core.entities.Group;
import co.chatchain.commons.infrastructure.InfrastructureModule;
import co.chatchain.commons.interfaces.IChatChainHubConnection;
import co.chatchain.dc.commands.StatsCommand;
import co.chatchain.dc.configs.AbstractConfig;
import co.chatchain.dc.configs.GroupsConfig;
import co.chatchain.dc.configs.MainConfig;
import co.chatchain.dc.messages.handlers.JDAMessages;
import co.chatchain.dc.serializers.GroupTypeSerializer;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.TypeToken;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Our bot's main class.
 */
public class ChatChainDC
{

    @Getter
    private IChatChainHubConnection connection = null;

    @Getter
    private JDA jda;

    @Getter
    private MainConfig mainConfig;

    @Getter
    private GroupsConfig groupsConfig;

    @Getter
    @Setter
    private Client client;

    @Getter
    private final Injector injector;

    /**
     * KV map of RequestIDs to Channel IDs
     */
    @Getter
    private final Cache<String, String> statsRequestsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    /**
     * The constructor for our bot.
     */
    private ChatChainDC()
    {

        File configDir = new File(System.getProperty("user.dir") + "/configs/");

        if (!configDir.exists())
        {
            //noinspection ResultOfMethodCallIgnored
            configDir.mkdirs();

            if (!configDir.getParentFile().exists())
            {
                System.out.println("Couldn't create config directory!");
                new IOException().printStackTrace();
            }
        }

        final Path mainConfigPath = configDir.toPath().resolve("main.json");

        System.out.println(mainConfigPath);

        //noinspection UnstableApiUsage
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(Group.class), new GroupTypeSerializer());

        mainConfig = AbstractConfig.getConfig(mainConfigPath, MainConfig.class,
                GsonConfigurationLoader.builder().setPath(mainConfigPath).build());

        final Path groupsConfigPath = configDir.toPath().resolve("groups.json");
        groupsConfig = AbstractConfig.getConfig(groupsConfigPath, GroupsConfig.class,
                GsonConfigurationLoader.builder().setPath(groupsConfigPath).build());

        Path formattingConfigPath = configDir.toPath().resolve("formatting.json");
        if (mainConfig.getAdvancedFormatting())
        {
            formattingConfigPath = configDir.toPath().resolve("advanced-formatting.json");
        }

        injector = Guice.createInjector(new HubModule(), new InfrastructureModule(), new ConfigurationModule(formattingConfigPath.toFile(), mainConfig.getAdvancedFormatting()), new ChatChainDCModule(this));

        try
        {
            CommandClientBuilder builder = new CommandClientBuilder();

            builder.setPrefix("!");
            builder.setAlternativePrefix("!ccdc");
            builder.setOwnerId("000000000000000000");

            builder.addCommand(new StatsCommand(this));

            jda = JDABuilder.createDefault(mainConfig.getDiscordClientId())
              .addEventListeners(builder.build())
              .build();
        }
        catch (LoginException e)
        {
            System.out.println("Could not initiate discord connection");
            e.printStackTrace();
            return;
        }

        jda.addEventListener(new JDAMessages(this));

        connection = injector.getInstance(ChatChainHubConnection.class);
        connection.connect();

        System.out.println("Connection Established: " + connection.getConnectionState());
    }

    /**
     * The start point for our bot.
     *
     * @param args the arguments fed to the JVM on init.
     */
    public static void main(String[] args)
    {
        new ChatChainDC();
    }
}
