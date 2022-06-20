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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class RandomItemDropsWhenKilled extends JavaPlugin {
    private static final Random random = new Random();

    @Override
    public void onEnable() {
        // 死亡不掉落
        new AlkaidEvent(this).simple()
                .event(WorldLoadEvent.class)
                .listener(event -> {
                    event.getWorld().setGameRule(GameRule.KEEP_INVENTORY, true);
                    getLogger().info(event.getWorld().getName() + " 的死亡不掉落已开启");
                })
                .priority(EventPriority.HIGHEST)
                .ignore(false)
                .register();

        // 进服欢迎
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

        // 死亡随机掉落物品
        new AlkaidEvent(this).simple()
                .event(PlayerDeathEvent.class)
                .listener(event -> {
                    Player player = event.getEntity();
                    PlayerInventory inventory = player.getInventory();
                    if (!hasPlunderItem(inventory)) return;
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

    /**
     * 检查玩家背包是否携带掠夺物品
     *
     * @return true:携带      false:不携带
     */
    public boolean hasPlunderItem(PlayerInventory inventory) {
        AtomicBoolean has = new AtomicBoolean(false);
        inventory.forEach(itemStack -> {
            if (itemStack == null || itemStack.getType().isAir()) return;
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta == null) return;
            List<String> lore = itemMeta.getLore();
            if (lore == null) return;
            lore.stream().filter(s -> s.contains("[掠夺]")).forEach(s -> has.set(true));
        });
        return has.get();
    }

}
