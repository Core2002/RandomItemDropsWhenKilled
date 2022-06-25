package fun.fifu.ridwk;

import com.alkaidmc.alkaid.bukkit.command.AlkaidCommand;
import com.alkaidmc.alkaid.bukkit.event.AlkaidEvent;

import lombok.Data;
import lombok.experimental.Accessors;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
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
import java.util.function.Consumer;

public class RandomItemDropsWhenKilled extends JavaPlugin implements Listener {
    private static final Random random = new Random();
    public static final Map<Player, Player> damageMap = new HashMap<>();

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
        makePlayerConmmand("ridwk.add-plunder", "ridwkAddPlunder", res -> {
            ItemStack itemInMainHand = res.player().getInventory().getItemInMainHand();
            if (itemInMainHand.getType().isAir())
                return;
            makePlunderItem(itemInMainHand);
            res.player().sendMessage("已为物品添加" + PluginConfig.INSTEN_CONFIG.getPlunderTag() + "属性");
        });

        // TODO 临时命令：查看手上物品耐久
        makePlayerConmmand("ridwk.add-plunder", "readDurable", res -> {
            ItemStack itemInMainHand = res.player().getInventory().getItemInMainHand();
            if (itemInMainHand.getType().isAir())
                return;
            res.player().sendMessage("物品耐久为" + readItemDurable(itemInMainHand));
        });

        // TODO 临时命令：设置手上物品耐久
        makePlayerConmmand("ridwk.add-plunder", "writeDurable", res -> {
            ItemStack itemInMainHand = res.player().getInventory().getItemInMainHand();
            if (itemInMainHand.getType().isAir())
                return;
            var num = Integer.parseInt(res.args()[1]);
            writeItemDurable(itemInMainHand, num);
            res.player().sendMessage("物品耐久已经设置为" + num);
        });

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
     * 检查玩家背包是否携带掠夺物品,若检查到，则消费该物品一个耐久
     *
     * @return true:携带 false:不携带劫夺图腾
     */
    public boolean hasPlunderItemAndSpend(Player player) {
        AtomicBoolean has = new AtomicBoolean(false);
        player.getInventory().forEach(itemStack -> {
            if (itemStack == null || itemStack.getType().isAir())
                return;
            if (hasPlunderItem(itemStack)) {
                var num = readItemDurable(itemStack);
                if (--num > 0) {
                    has.set(true);
                    writeItemDurable(itemStack, num);
                }else{
                    itemStack.setAmount(0);
                    player.sendMessage("因为使用次数耗尽，你消耗了一个掠夺符");
                }
            }

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

        writeItemDurable(itemStack, PluginConfig.INSTEN_CONFIG.getDefaultDurableNum());
    }

    /**
     * 让指定玩家随机掉落物品
     * 
     * @param player 目标玩家
     */
    public void randomDropItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        Player damage = damageMap.get(player);
        if (damage == null)
            return;
        if (!hasPlunderItemAndSpend(damage))
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

    /**
     * 读取指定物品堆的耐久
     * 
     * @param itemStack 要读取耐久的物品堆
     * @return 该物品的剩余使用次数
     */
    public int readItemDurable(ItemStack itemStack) {
        var itemMeta = itemStack.getItemMeta();
        if (!itemMeta.hasLore())
            return -1;
        String text = itemMeta.getLore().stream()
                .filter(s -> s.contains(PluginConfig.INSTEN_CONFIG.getDurableTag()))
                .findFirst().orElse("-1");
        if (text == null)
            return 0;
        return Integer.parseInt(text.replaceAll(PluginConfig.INSTEN_CONFIG.getDurableTag(), ""));
    }

    /**
     * 设置指定物品堆的耐久
     * 
     * @param itemStack 要设置耐久的物品堆
     */
    public void writeItemDurable(ItemStack itemStack, int num) {
        var itemMeta = itemStack.getItemMeta();
        var lore = itemMeta.getLore();
        if (lore == null)
            lore = new ArrayList<>();
        if (readItemDurable(itemStack) == -1) {
            lore.add(PluginConfig.INSTEN_CONFIG.durableTag + num);
        } else {
            for (int i = 0; i < lore.size(); i++)
                if (lore.get(i).contains(PluginConfig.INSTEN_CONFIG.getDurableTag()))
                    lore.set(i, PluginConfig.INSTEN_CONFIG.durableTag + num);
        }
        if (num <= 0) {
            for (int i = 0; i < lore.size(); i++)
                if (lore.get(i).contains(PluginConfig.INSTEN_CONFIG.getDurableTag()))
                    lore.remove(i);
        }
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
    }

    @Data
    @Accessors(fluent = true, chain = true)
    private static class Res {
        private Player player;
        private String[] args;
    }

    /**
     * 构建一个伪命令，玩家用聊天的形式触发
     * 
     * @param permission 触发命令所需的权限
     * @param command    命令内容
     * @param exec       触发后执行的代码
     */
    public static void makePlayerConmmand(String permission, String command, Consumer<Res> exec) {
        new AlkaidEvent(RandomItemDropsWhenKilled.randomItemDropsWhenKilled).simple()
                .event(AsyncPlayerChatEvent.class)
                .listener(event -> {
                    var args = event.getMessage().split(" ");
                    if (!args[0].equalsIgnoreCase(command))
                        return;
                    Player player = event.getPlayer();
                    if (player.hasPermission(permission)) {
                        exec.accept(new Res()
                                .player(player)
                                .args(args));
                    } else {
                        player.sendMessage("你没有 " + permission + " 权限，无法使用 " + command + " 命令");
                        return;
                    }
                    event.setCancelled(true);
                })
                .priority(EventPriority.HIGHEST)
                .ignore(true)
                .register();
    }

}
