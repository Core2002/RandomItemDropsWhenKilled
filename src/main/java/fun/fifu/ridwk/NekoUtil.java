package fun.fifu.ridwk;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.alkaidmc.alkaid.bukkit.event.AlkaidEvent;

import lombok.Data;
import lombok.experimental.Accessors;

public class NekoUtil {

    /**
     * 检查玩家背包内是否携带某种标签的物品
     *
     * @return true:携带 false:不携带
     */
    public static boolean hasTagItem(PlayerInventory inventory, String tag) {
        AtomicBoolean has = new AtomicBoolean(false);
        inventory.forEach(itemStack -> {
            if (itemStack == null || itemStack.getType().isAir())
                return;
            if (hasTagItem(itemStack, tag)) {
                var num = readItemNumericProperties(itemStack, tag);
                if (num >= 0) {
                    has.set(true);
                    writeItemNumericProperties(itemStack, tag, num--);
                    System.out.println("剩余次数设置为" + num);
                }
            }

        });
        return has.get();
    }

    /**
     * 检查物品是否携带指定标签
     *
     * @return true:携带 false:不携带
     */
    public static boolean hasTagItem(ItemStack itemStack, String tag) {
        AtomicBoolean is = new AtomicBoolean(false);
        if (itemStack == null || itemStack.getType().isAir())
            return false;
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null)
            return false;
        List<String> lore = itemMeta.getLore();
        if (lore == null)
            return false;
        lore.stream().filter(s -> s.contains(tag)).forEach(s -> is.set(true));
        return is.get();
    }

    /**
     * 给物品标记上某种标签
     *
     * @param itemStack 待标记的物品
     */
    public static void makeTagItem(ItemStack itemStack, String tag) {
        if (hasTagItem(itemStack, tag))
            return;
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null)
            return;
        List<String> lore = itemMeta.getLore();
        if (lore == null)
            lore = new ArrayList<>();
        lore.add(0, tag);
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
    }

    /**
     * 读取指定物品堆的某数值属性
     * 
     * @param itemStack 要读取数值属性的物品堆
     * @return 读取出的数值，-1为不含数值属性
     */
    public static int readItemNumericProperties(ItemStack itemStack, String tag) {
        var itemMeta = itemStack.getItemMeta();
        if (!itemMeta.hasLore())
            return -1;
        String text = itemMeta.getLore().stream()
                .filter(s -> s.contains(tag))
                .findFirst().orElse("-1");
        if (text == null)
            return 0;
        return Integer.parseInt(text.replaceAll(tag, ""));
    }

    /**
     * 设置指定物品堆的某数值属性
     * 
     * @param itemStack 要设置的物品堆
     */
    public static void writeItemNumericProperties(ItemStack itemStack, String tag, int num) {
        var itemMeta = itemStack.getItemMeta();
        var lore = itemMeta.getLore();
        if (lore == null)
            lore = new ArrayList<>();
        if (readItemNumericProperties(itemStack, tag) == -1) {
            lore.add(tag + num);
        } else {
            for (int i = 0; i < lore.size(); i++)
                if (lore.get(i).contains(tag))
                    lore.set(i, tag + num);
        }
        if (num <= 0) {
            for (int i = 0; i < lore.size(); i++)
                if (lore.get(i).contains(tag))
                    lore.remove(i);
        }
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
    }

    @Data
    @Accessors(fluent = true, chain = true)
    public static class Res {
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

    private static final Random random = new Random();

    /**
     * 让指定玩家随机掉落物品
     * 
     * @param player 目标玩家
     */
    public void randomDropItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        Player damage = RandomItemDropsWhenKilled.damageMap.get(player);
        if (damage == null)
            return;
        inventory.forEach(itemStack -> {
            if (random.nextDouble() > 0.3)
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
