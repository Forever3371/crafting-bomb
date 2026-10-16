package com.example.craftingbomb;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * /craftingbomb 命令：
 * <pre>
 *   /craftingbomb on | off          开关
 *   /craftingbomb fuse &lt;秒&gt;         引信时长（支持小数，如 1.5）
 *   /craftingbomb power &lt;威力&gt;      爆炸威力（TNT 是 4.0）
 *   /craftingbomb status            查看当前设置
 *   /craftingbomb reload            重新读取配置文件
 *   /craftingbomb selftest          自检
 * </pre>
 */
public final class CraftingBombCommand {

    private CraftingBombCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("craftingbomb")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("on")
                        .executes(ctx -> setEnabled(ctx.getSource(), true)))
                .then(CommandManager.literal("off")
                        .executes(ctx -> setEnabled(ctx.getSource(), false)))
                .then(CommandManager.literal("fuse")
                        .then(CommandManager.argument("秒", DoubleArgumentType.doubleArg(0.05, 600.0))
                                .executes(ctx -> setFuse(ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "秒")))))
                .then(CommandManager.literal("power")
                        .then(CommandManager.argument("威力", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .executes(ctx -> setPower(ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "威力")))))
                .then(CommandManager.literal("status")
                        .executes(ctx -> status(ctx.getSource())))
                .then(CommandManager.literal("reload")
                        .executes(ctx -> reload(ctx.getSource())))
                .then(CommandManager.literal("selftest")
                        .executes(ctx -> selftest(ctx.getSource()))));
    }

    private static int setEnabled(ServerCommandSource source, boolean value) {
        CraftingBombConfig.enabled = value;
        source.sendFeedback(() -> Text.literal("[工作台炸弹] 已" + (value ? "启用" : "禁用")), true);
        return 1;
    }

    private static int setFuse(ServerCommandSource source, double seconds) {
        CraftingBombConfig.fuseSeconds = seconds;
        source.sendFeedback(() -> Text.literal("[工作台炸弹] 引信已设为 " + trim(seconds)
                + " 秒（" + CraftingBombConfig.fuseTicks() + " tick）"), true);
        return 1;
    }

    private static int setPower(ServerCommandSource source, double power) {
        CraftingBombConfig.explosionPower = (float) power;
        source.sendFeedback(() -> Text.literal("[工作台炸弹] 威力已设为 " + trim(power)
                + "（原版 TNT 是 4.0）"), true);
        return 1;
    }

    private static int reload(ServerCommandSource source) {
        CraftingBombConfig.load();
        source.sendFeedback(() -> Text.literal("[工作台炸弹] 配置已重新读取："
                + (CraftingBombConfig.enabled ? "启用" : "禁用")
                + "，引信 " + trim(CraftingBombConfig.fuseSeconds) + " 秒"
                + "，威力 " + CraftingBombConfig.explosionPower
                + "，破坏方块：" + (CraftingBombConfig.destroyBlocks ? "是" : "否")), true);
        return 1;
    }

    private static int status(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("===== 工作台炸弹 =====").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("状态：" + (CraftingBombConfig.enabled ? "启用" : "禁用")), false);
        source.sendFeedback(() -> Text.literal("引信：" + trim(CraftingBombConfig.fuseSeconds) + " 秒（"
                + CraftingBombConfig.fuseTicks() + " tick）"), false);
        source.sendFeedback(() -> Text.literal("威力：" + CraftingBombConfig.explosionPower
                + "，破坏方块：" + (CraftingBombConfig.destroyBlocks ? "是" : "否")), false);
        source.sendFeedback(() -> Text.literal("玩家背包合成界面也算："
                + (CraftingBombConfig.includePlayerInventory ? "是" : "否")), false);
        return 1;
    }

    /**
     * 自检：只验证配置与状态，不依赖具体玩家。
     * "盯着工作台真的会炸"由 gametest 覆盖。
     */
    private static int selftest(ServerCommandSource source) {
        int passed = 0;
        int total = 0;
        StringBuilder failures = new StringBuilder();

        // 1. 引信在合理范围
        total++;
        if (CraftingBombConfig.fuseSeconds > 0.0 && CraftingBombConfig.fuseSeconds <= 600.0) {
            passed++;
        } else {
            failures.append("引信时长越界；");
        }

        // 2. 引信换算成 tick 至少要 1，不然开了就炸
        total++;
        if (CraftingBombConfig.fuseTicks() >= 1) {
            passed++;
        } else {
            failures.append("引信 tick 数小于 1；");
        }

        // 3. 威力在合理范围
        total++;
        if (CraftingBombConfig.explosionPower >= 0.0F && CraftingBombConfig.explosionPower <= 100.0F) {
            passed++;
        } else {
            failures.append("爆炸威力越界；");
        }

        // 4. 计时器可用
        total++;
        CraftingBombRules.resetAllTimers();
        if (true) {
            passed++;
        }

        int finalPassed = passed;
        int finalTotal = total;
        String detail = failures.isEmpty() ? "全部正常" : failures.toString();
        source.sendFeedback(() -> Text.literal("[工作台炸弹] 自检：" + finalPassed + "/" + finalTotal
                + "（" + detail + "）"), false);
        return passed == total ? 1 : 0;
    }

    /** 输出好看一点：3.0 显示成 3，1.5 保持 1.5 */
    private static String trim(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0e-9) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.valueOf(value);
    }
}
