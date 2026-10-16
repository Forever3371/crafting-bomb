package com.example.craftingbomb;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crafting Bomb（工作台炸弹）
 * <p>
 * 打开工作台超过指定秒数就炸。默认 3 秒、威力 3.0（TNT 是 4.0）、会破坏方块。
 * <p>
 * 实现方式：服务端每个 tick 检查每个玩家的 {@code currentScreenHandler} 是不是工作台容器，
 * 是的话给他累加计时，不是就清零。计时到点就炸。
 * <p>
 * 为什么放服务端：客户端检测可以被绕过，而且计时精度也不该依赖客户端帧率。
 */
public class CraftingBomb implements ModInitializer {

    public static final String MOD_ID = "crafting_bomb";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CraftingBombConfig.load();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!CraftingBombConfig.enabled) {
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                CraftingBombRules.tickPlayer(player);
            }
        });

        if (CraftingBombConfig.enableCommand) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                    CraftingBombCommand.register(dispatcher));
        }

        LOGGER.info("[Crafting Bomb] 已加载：打开工作台超过 {} 秒就会爆炸（威力 {}，{}）",
                CraftingBombConfig.fuseSeconds,
                CraftingBombConfig.explosionPower,
                CraftingBombConfig.destroyBlocks ? "会破坏方块" : "不破坏方块");
    }
}
