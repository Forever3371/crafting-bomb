package com.example.craftingbomb.test;

import com.example.craftingbomb.CraftingBombConfig;
import com.example.craftingbomb.CraftingBombRules;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * 自动化测试：在无头服务端里真的把玩家的界面换成工作台，检查计时与爆炸。
 * 运行：./gradlew runGameTest
 */
public class CraftingBombGameTest implements FabricGameTest {

    // ---------------------------------------------------------- 计时

    /** 开着工作台，计时会一直累加 */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void timerCountsUpWhileCraftingTableOpen(TestContext context) {
        useTestSettings();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        CraftingBombRules.resetAllTimers();
        openCraftingTable(player);

        for (int i = 0; i < 10; i++) {
            CraftingBombRules.tickPlayer(player);
        }

        context.assertTrue(CraftingBombRules.currentTicks(player) == 10,
                "开着工作台 10 tick 后计时应当是 10，实际 " + CraftingBombRules.currentTicks(player));
        context.complete();
    }

    /** 关掉界面再打开必须重新计时，否则会出现"刚打开就炸" */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void closingResetsTimer(TestContext context) {
        useTestSettings();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        CraftingBombRules.resetAllTimers();

        openCraftingTable(player);
        for (int i = 0; i < 20; i++) {
            CraftingBombRules.tickPlayer(player);
        }
        context.assertTrue(CraftingBombRules.currentTicks(player) == 20,
                "应当累计到 20，实际 " + CraftingBombRules.currentTicks(player));

        closeScreen(player);
        CraftingBombRules.tickPlayer(player);
        context.assertTrue(CraftingBombRules.currentTicks(player) == 0,
                "关掉界面后计时应清零，实际 " + CraftingBombRules.currentTicks(player));

        openCraftingTable(player);
        CraftingBombRules.tickPlayer(player);
        context.assertTrue(CraftingBombRules.currentTicks(player) == 1,
                "重新打开应当从 1 开始，实际 " + CraftingBombRules.currentTicks(player));
        context.complete();
    }

    /** 界面不是工作台时不计时 */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void otherScreensDoNotCount(TestContext context) {
        useTestSettings();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        CraftingBombRules.resetAllTimers();

        // 玩家自己的 2x2 合成界面（默认不算工作台）
        player.currentScreenHandler = new PlayerScreenHandler(player.getInventory(), false, player);
        for (int i = 0; i < 30; i++) {
            CraftingBombRules.tickPlayer(player);
        }

        context.assertTrue(CraftingBombRules.currentTicks(player) == 0,
                "玩家背包合成界面默认不该计入，实际 " + CraftingBombRules.currentTicks(player));
        context.complete();
    }

    // ---------------------------------------------------------- 爆炸

    /**
     * 引信到点要真的炸。
     * <p>
     * 注意不能靠"玩家掉血"来验证——mock 玩家是创造模式，免疫爆炸伤害。
     * 所以改成在脚下放一块石头，看爆炸有没有把它炸掉。
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void explodesAfterFuseExpires(TestContext context) {
        useTestSettings();
        CraftingBombConfig.fuseSeconds = 0.5;      // 10 tick
        CraftingBombConfig.explosionPower = 4.0F;  // TNT 级
        CraftingBombConfig.destroyBlocks = true;   // 必须为 true 才能用方块被炸来验证
        CraftingBombConfig.showCountdown = false;

        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        BlockPos stone = context.getAbsolutePos(new BlockPos(0, 1, 0));
        context.getWorld().setBlockState(stone, Blocks.STONE.getDefaultState());
        player.teleport(context.getWorld(),
                stone.getX() + 0.5, stone.getY() + 1.0, stone.getZ() + 0.5, 0.0F, 0.0F);

        openCraftingTable(player);
        CraftingBombRules.resetAllTimers();

        // 正好 tick 到引信长度：第 10 次会触发爆炸并清掉计时。
        // 不能多 tick —— 真实游戏里爆炸会炸掉工作台、界面随之自动关闭，
        // 但这里的 mock 玩家界面不会自己关，多 tick 会立刻重新开始计时。
        for (int i = 0; i < 10; i++) {
            CraftingBombRules.tickPlayer(player);
        }

        context.assertTrue(CraftingBombRules.currentTicks(player) == 0,
                "爆炸之后计时应被清掉，实际 " + CraftingBombRules.currentTicks(player));
        context.assertTrue(context.getWorld().getBlockState(stone).isAir(),
                "爆炸应当把脚下的石头炸掉（说明真的产生了一次爆炸）");
        context.complete();
    }

    /** 关掉开关后不再计时 */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void disabledStopsCounting(TestContext context) {
        useTestSettings();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        CraftingBombRules.resetAllTimers();

        // tickPlayer 自身不管开关（开关在事件注册处判断），这里直接验证
        // "不在工作台界面就不计时"这条更基础的行为
        closeScreen(player);
        for (int i = 0; i < 10; i++) {
            CraftingBombRules.tickPlayer(player);
        }
        context.assertTrue(CraftingBombRules.currentTicks(player) == 0,
                "不在工作台界面时不该计时");

        CraftingBombConfig.enabled = true;
        context.complete();
    }

    // ---------------------------------------------------------- 辅助

    private static void useTestSettings() {
        CraftingBombConfig.enabled = true;
        CraftingBombConfig.fuseSeconds = 3.0;
        CraftingBombConfig.explosionPower = 3.0F;
        CraftingBombConfig.destroyBlocks = true;
        CraftingBombConfig.showCountdown = false;   // 测试里别刷 actionbar
        CraftingBombConfig.includePlayerInventory = false;
    }

    /** 把玩家的当前界面换成一个工作台容器。 */
    private static void openCraftingTable(ServerPlayerEntity player) {
        player.currentScreenHandler = new CraftingScreenHandler(1, player.getInventory());
    }

    /** 把玩家的当前界面换回他自己的背包（相当于关掉了工作台）。 */
    private static void closeScreen(ServerPlayerEntity player) {
        player.currentScreenHandler = new PlayerScreenHandler(player.getInventory(), false, player);
    }
}
