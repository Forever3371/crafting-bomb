package com.example.craftingbomb;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 极简配置：读写 config/crafting_bomb.properties。
 */
public final class CraftingBombConfig {

    /** 总开关。 */
    public static boolean enabled = true;

    /** 引信时长（秒）。打开工作台超过这个时间就炸。 */
    public static double fuseSeconds = 3.0;

    /** 爆炸威力。原版 TNT 是 4.0。 */
    public static float explosionPower = 3.0F;

    /** 爆炸是否破坏方块。关掉的话只伤害玩家/生物，地形完好。 */
    public static boolean destroyBlocks = true;

    /** 是否把"玩家自己的 2x2 合成界面"也算作工作台。默认不算。 */
    public static boolean includePlayerInventory = false;

    /** 是否注册 /craftingbomb 命令。 */
    public static boolean enableCommand = true;

    private static final String FILE_NAME = "crafting_bomb.properties";

    private CraftingBombConfig() {
    }

    /** 引信换算成 tick（20 tick = 1 秒）。 */
    public static int fuseTicks() {
        return Math.max(1, (int) Math.round(fuseSeconds * 20.0));
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties props = new Properties();

        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, defaultContent(), StandardCharsets.UTF_8);
                CraftingBomb.LOGGER.info("[Crafting Bomb] 已生成默认配置：{}", path);
            }
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
        } catch (IOException e) {
            CraftingBomb.LOGGER.warn("[Crafting Bomb] 读取配置失败，使用内置默认值", e);
        }

        enabled = readBoolean(props, "enabled", true);
        fuseSeconds = readDouble(props, "fuse_seconds", 3.0, 0.05, 600.0);
        explosionPower = (float) readDouble(props, "explosion_power", 3.0, 0.0, 100.0);
        destroyBlocks = readBoolean(props, "destroy_blocks", true);
        includePlayerInventory = readBoolean(props, "include_player_inventory", false);
        enableCommand = readBoolean(props, "enable_command", true);
    }

    private static boolean readBoolean(Properties props, String key, boolean fallback) {
        String raw = props.getProperty(key);
        return raw == null ? fallback : Boolean.parseBoolean(raw.trim());
    }

    private static double readDouble(Properties props, String key, double fallback, double min, double max) {
        String raw = props.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < min || value > max) {
                CraftingBomb.LOGGER.warn("[Crafting Bomb] 配置项 {}={} 超出范围 [{}, {}]，使用默认值 {}",
                        key, value, min, max, fallback);
                return fallback;
            }
            return value;
        } catch (NumberFormatException e) {
            CraftingBomb.LOGGER.warn("[Crafting Bomb] 配置项 {}={} 不是数字，使用默认值 {}", key, raw, fallback);
            return fallback;
        }
    }

    private static String defaultContent() {
        return """
                # Crafting Bomb（工作台炸弹）配置
                # 打开工作台超过指定秒数就爆炸。

                # 总开关
                enabled=true

                # 引信时长（秒）。打开工作台超过这个时间就炸。
                # 3.0 就是"超过 3 秒爆炸"。关掉界面会重新计时，不会累积。
                fuse_seconds=3.0

                # 爆炸威力。原版 TNT 是 4.0，默认 3.0 略小一点。
                # 调到 0 就是不造成任何伤害，只当个提示。
                explosion_power=3.0

                # 爆炸是否破坏方块
                # true  = 炸掉工作台和周围一片地形（正常爆炸伤害）
                # false = 只伤害玩家/生物，地形完好
                destroy_blocks=true

                # 是否把"玩家自己的 2x2 合成界面"也算作工作台
                # 默认 false：只管真正的工作台方块
                include_player_inventory=false

                # 是否注册 /craftingbomb 命令
                enable_command=true
                """;
    }
}
