# Crafting Bomb（工作台炸弹）

Minecraft **1.21.1** / **Fabric** 模组：**打开工作台超过指定秒数就爆炸**。默认 3 秒。

## 效果

- 打开工作台后，快捷栏上方开始显示倒计时（`⚠ 工作台将在 2.3 秒后爆炸 ⚠`）
- 超过 **3 秒** → 原地爆炸，默认威力 **3.0**（原版 TNT 是 4.0），**会破坏方块**
- **关掉界面会重新计时**，所以"开一下、关掉、再开"不会累积

## 配置

首次启动生成 `config/crafting_bomb.properties`：

```properties
# 总开关
enabled=true

# 引信时长（秒）。打开工作台超过这个时间就炸。
fuse_seconds=3.0

# 爆炸威力。原版 TNT 是 4.0。调到 0 就是不造成伤害，只当提示。
explosion_power=3.0

# 爆炸是否破坏方块
# true  = 炸掉工作台和周围一片地形
# false = 只伤害玩家/生物，地形完好
destroy_blocks=true

# 是否在快捷栏上方显示倒计时
show_countdown=true

# 是否把"玩家自己的 2x2 合成界面"也算作工作台（默认只管真正的工作台方块）
include_player_inventory=false

# 是否注册 /craftingbomb 命令
enable_command=true
```

## 游戏内命令

```
/craftingbomb on | off        开关
/craftingbomb fuse <秒>        引信时长（支持小数，如 1.5）
/craftingbomb power <威力>     爆炸威力
/craftingbomb status          查看当前设置
/craftingbomb reload          重新读取配置文件
/craftingbomb selftest        自检
```

需要权限等级 2（单机开作弊即可，服务器需要 OP）。

## 实现要点

- **计时放服务端**：客户端检测能被绕过，而且倒计时不该依赖客户端帧率
- **关界面立刻清零**：否则"开→关→再开"会累积，出现"刚打开就炸"
- **爆炸源是玩家自己**：这样击杀归属说得通
- **只认真正的工作台方块**：判断 `currentScreenHandler` 是不是 `CraftingScreenHandler`

## 测试

```bash
./gradlew build          # 顺带跑 gametest
./gradlew runGameTest    # 只跑测试（5 项）
```

gametest 覆盖：

- 开着工作台，计时会一直累加
- **关掉界面再打开会重新计时**（防"刚打开就炸"）
- 玩家背包的 2x2 合成界面默认不计入
- **引信到点真的爆炸** —— 这条不能靠"玩家掉血"验证：gametest 的 mock 玩家是创造模式，
  免疫爆炸伤害。所以改成在脚下放一块石头，检查爆炸有没有把它炸掉
- 不在工作台界面时不计时

生产验证：`tools/production-test/` 数据包，服务器启动时自动跑 `/craftingbomb selftest`
并用 `execute store success` 断言结果，日志出现 `PASS_CRAFTINGBOMB_SELFTEST` 即为通过。

## 已知限制

- **只识别原版工作台**（`CraftingScreenHandler`）。其他模组加的合成台（各种工作台变体）
  不会被计入——它们有各自的容器类
- 爆炸会真的毁地形（`destroy_blocks=true`），想只吓人不毁图就改成 `false`

## 许可

MIT
