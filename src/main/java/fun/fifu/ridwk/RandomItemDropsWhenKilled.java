package fun.fifu.ridwk;

import com.alkaidmc.alkaid.bukkit.event.AlkaidEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class RandomItemDropsWhenKilled extends JavaPlugin {
    private static final Random random = new Random();

    @Override
    public void onEnable() {
        new AlkaidEvent(this).simple()
                .event(WorldLoadEvent.class)
                .listener(event -> {
                    event.getWorld().setGameRule(GameRule.KEEP_INVENTORY, true);
                    getLogger().info(event.getWorld().getName() + " 的死亡不掉落已开启");
                })
                .priority(EventPriority.HIGHEST)
                .ignore(false)
                .register();

        new AlkaidEvent(this).simple()
                .event(PlayerJoinEvent.class)
                .listener(event -> {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.sendMessage("欢迎" + event.getPlayer().getDisplayName() + "来到服务器");
                    });
                })
                .priority(EventPriority.HIGHEST)
                .ignore(false)
                .register();

        new AlkaidEvent(this).simple()
                .event(PlayerDeathEvent.class)
                .listener(event -> {
                    Player player = event.getEntity();
                    PlayerInventory inventory = player.getInventory();
                    inventory.forEach(itemStack -> {
                        if (random.nextDouble() > 0.3) return;
                        if (itemStack == null || itemStack.getType().isAir()) return;
                        var num = itemStack.getAmount() - 1;
                        if (num < 0) num = 0;
                        ItemStack drop = itemStack.clone();
                        drop.setAmount(1);
                        itemStack.setAmount(num);
                        player.getWorld().dropItem(player.getLocation(), drop);
                    });
                })
                .priority(EventPriority.HIGHEST)
                .ignore(false)
                .register();
    }

}
