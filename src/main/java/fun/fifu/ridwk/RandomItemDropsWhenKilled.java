package fun.fifu.ridwk;

import com.alkaidmc.alkaid.bukkit.command.AlkaidCommand;
import com.alkaidmc.alkaid.bukkit.event.AlkaidEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RandomItemDropsWhenKilled extends JavaPlugin implements Listener {
    private static final Random random = new Random();
    private static final Map<Player, Player> damageMap = new HashMap<>();

    public static RandomItemDropsWhenKilled randomItemDropsWhenKilled;

    @Override
    public void onLoad() {
        randomItemDropsWhenKilled = this;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

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
                .listener(event -> randomDropItems(event.getEntity()))
                .priority(EventPriority.HIGHEST)
                .ignore(false)
                .register();

        // 命令：给物品标记掠夺属性
        new AlkaidCommand(this).simple()
                .command("ridwkAddPlunder")
                .description("将手上的物品标记为" + PluginConfig.INSTEN_CONFIG.getPlunderTag() + "物品")
                .permission("ridwk.add-plunder")
                .usage("/ridwkAddPlunder 将手上的物品标记为" + PluginConfig.INSTEN_CONFIG.getPlunderTag() + "物品")
                .executor((sender, command, label, args) -> {
                    if (!(sender instanceof Player player))
                        return true;
                    ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
                    if (itemInMainHand.getType().isAir())
                        return true;
                    makePlunderItem(itemInMainHand);
                    player.sendMessage("已为物品添加" + PluginConfig.INSTEN_CONFIG.getPlunderTag() + "属性");
                    return true;
                })
                .register();

        // TODO 临时命令：给物品标记掠夺属性
        new AlkaidEvent(this).simple()
                .event(AsyncPlayerChatEvent.class)
                .listener(event -> {
                    if (!event.getMessage().equalsIgnoreCase("ridwkAddPlunder"))
                        return;
                    Player player = event.getPlayer();
                    if (player.hasPermission("ridwk.add-plunder")) {
                        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
                        if (itemInMainHand.getType().isAir())
                            return;
                        makePlunderItem(itemInMainHand);
                        player.sendMessage("已为物品添加" + PluginConfig.INSTEN_CONFIG.getPlunderTag() + "属性");
                    } else {
                        player.sendMessage("你没有 ridwk.add-plunder 权限，无法使用命令");
                    }
                    event.setCancelled(true);
                })
                .priority(EventPriority.HIGHEST)
                .ignore(true)
                .register();
    }

    @Override
    public void onDisable() {
        damageMap.clear();
    }

    @EventHandler
    public void tagDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof Player damage) {
            damageMap.put(player, damage);
        }
    }

    /**
     * 检查玩家背包是否携带掠夺物品
     *
     * @return true:携带 false:不携带
     */
    public boolean hasPlunderItem(PlayerInventory inventory) {
        AtomicBoolean has = new AtomicBoolean(false);
        inventory.forEach(itemStack -> {
            if (itemStack == null || itemStack.getType().isAir())
                return;
            if (hasPlunderItem(itemStack))
                has.set(true);
        });
        return has.get();
    }

    /**
     * 检查物品是否携带掠夺物品
     *
     * @return true:携带 false:不携带
     */
    public boolean hasPlunderItem(ItemStack itemStack) {
        AtomicBoolean is = new AtomicBoolean(false);
        if (itemStack == null || itemStack.getType().isAir())
            return false;
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null)
            return false;
        List<String> lore = itemMeta.getLore();
        if (lore == null)
            return false;
        lore.stream().filter(s -> s.contains(PluginConfig.INSTEN_CONFIG.getPlunderTag())).forEach(s -> is.set(true));
        return is.get();
    }

    /**
     * 标记物品为掠夺物品
     *
     * @param itemStack 待标记的物品
     */
    public void makePlunderItem(ItemStack itemStack) {
        if (hasPlunderItem(itemStack))
            return;
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null)
            return;
        List<String> lore = itemMeta.getLore();
        if (lore == null)
            lore = new ArrayList<>();
        lore.add(0, PluginConfig.INSTEN_CONFIG.getPlunderTag());
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
    }

    /**
     * 让指定玩家随机掉落物品
     * @param player 目标玩家
     */
    public void randomDropItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        Player damage = damageMap.get(player);
        if (damage == null)
            return;
        PlayerInventory damageInventory = damage.getInventory();
        if (!hasPlunderItem(damageInventory))
            return;
        inventory.forEach(itemStack -> {
            if (random.nextDouble() > PluginConfig.INSTEN_CONFIG.getDropProbability())
                return;
            if (itemStack == null || itemStack.getType().isAir())
                return;
            int dropNum = random.nextInt(itemStack.getAmount());
            int num = itemStack.getAmount() - dropNum;
            if (num < 0) {
                num = 0;
                dropNum = itemStack.getAmount();
            }
            if (dropNum == 0)
                return;
            ItemStack drop = itemStack.clone();
            drop.setAmount(dropNum);
            itemStack.setAmount(num);
            player.getWorld().dropItem(player.getLocation(), drop);
        });
    }

}
