package com.example.craftingbomb;

import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 核心逻辑：盯着工作台看太久就炸。
 * <p>
 * 单独抽出来是为了让命令自检和 gametest 调用同一套判断，而不是各写一遍。
 */
public final class CraftingBombRules {

    /** 每个玩家已经"盯着工作台"看了多少 tick。关掉界面就清掉，所以开→关→再开是重新计时的。 */
    private static final Map<UUID, Integer> TIMERS = new HashMap<>();

    private CraftingBombRules() {
    }

    /**
     * 每个 tick 对每个在线玩家调用一次。
     */
    public static void tickPlayer(ServerPlayerEntity player) {
        if (!isWatchingCraftingTable(player)) {
            TIMERS.remove(player.getUuid()); // 没在工作台界面，计时清零
            return;
        }

        int ticks = TIMERS.merge(player.getUuid(), 1, Integer::sum);
        int fuse = CraftingBombConfig.fuseTicks();

        if (ticks >= fuse) {
            TIMERS.remove(player.getUuid());
            explode(player);
            return;
        }

        if (CraftingBombConfig.showCountdown) {
            double remaining = (fuse - ticks) / 20.0;
            player.sendMessage(
                    Text.literal("⚠ 工作台将在 " + String.format("%.1f", remaining) + " 秒后爆炸 ⚠")
                            .formatted(remaining <= 1.0 ? Formatting.RED : Formatting.YELLOW),
                    true);
        }
    }

    /**
     * 玩家现在是不是正开着工作台。
     * <p>
     * 按配置也可以把"玩家自己的 2x2 合成界面"算进来——默认不算，因为那不是工作台。
     */
    public static boolean isWatchingCraftingTable(ServerPlayerEntity player) {
        if (player.currentScreenHandler instanceof CraftingScreenHandler) {
            return true;
        }
        return CraftingBombConfig.includePlayerInventory
                && player.currentScreenHandler instanceof PlayerScreenHandler;
    }

    /** 这个玩家当前累计了多少 tick（0 = 没在计时）。 */
    public static int currentTicks(ServerPlayerEntity player) {
        return TIMERS.getOrDefault(player.getUuid(), 0);
    }

    /** 清掉某个玩家的计时（命令与测试用）。 */
    public static void resetTimer(ServerPlayerEntity player) {
        TIMERS.remove(player.getUuid());
    }

    /** 清掉全部计时（测试用）。 */
    public static void resetAllTimers() {
        TIMERS.clear();
    }

    /**
     * 让指定玩家炸一次。
     * <p>
     * 爆炸源传这个玩家自己，这样击杀归属说得通；威力与是否破坏方块都走配置。
     */
    public static void explode(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        world.createExplosion(
                player,
                player.getX(), player.getY() + 0.5, player.getZ(),
                CraftingBombConfig.explosionPower,
                CraftingBombConfig.destroyBlocks
                        ? World.ExplosionSourceType.TNT
                        : World.ExplosionSourceType.NONE);

        CraftingBomb.LOGGER.info("[Crafting Bomb] {} 盯着工作台太久，炸了（威力 {}，{}）",
                player.getName().getString(), CraftingBombConfig.explosionPower,
                CraftingBombConfig.destroyBlocks ? "破坏方块" : "不破坏方块");
    }
}
