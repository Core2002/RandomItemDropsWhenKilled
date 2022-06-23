package fun.fifu.ridwk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.bukkit.plugin.Plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Data;

@Data
public class PluginConfig {
    public static PluginConfig INSTEN_CONFIG;
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static Plugin plugin = RandomItemDropsWhenKilled.randomItemDropsWhenKilled;

    Double dropProbability = 0.3;
    String plunderTag = "[掠夺]";
    String durableTag = "耐久:";
    Integer defaultDurableNum = 6;

    static {
        INSTEN_CONFIG = build();
    }

    private static PluginConfig build() {
        plugin.saveResource("config.json", false);
        File file = Arrays.stream(plugin.getDataFolder().listFiles())
                .filter(f -> f.getName().equals("config.json"))
                .findFirst().get();
        try (FileInputStream fis = new FileInputStream(file)) {
            return gson.fromJson(new String(fis.readAllBytes(), Charset.forName("UTF-8")), PluginConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private PluginConfig() {
        plugin.getLogger().info("成功加载配置文件：" + gson.toJson(this));
    }

}
