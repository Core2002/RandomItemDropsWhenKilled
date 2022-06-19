package fun.fifu.ridwk;

import com.alkaidmc.alkaid.bukkit.event.AlkaidEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class RandomItemDropsWhenKilled extends JavaPlugin {
    @Override
    public void onEnable() {
        new AlkaidEvent(this).simple()
                // 监听的事件
                .event(PlayerJoinEvent.class)
                // 事件处理器
                .listener(event -> {

                    event.getPlayer().sendMessage("欢迎" + event.getPlayer().getDisplayName() + "来到服务器");
                })
                // 事件优先级
                .priority(EventPriority.HIGHEST)
                // 忽略取消标志位
                .ignore(false)
                // 将事件注册到 Bukkit 事件系统
                .register();
    }
}
