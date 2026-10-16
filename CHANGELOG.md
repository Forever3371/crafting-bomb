# 更新日志

## [1.0.0] - 2025

### 新增

- **打开工作台超过指定秒数就爆炸**：默认 3 秒、威力 3.0、会破坏方块。
- **快捷栏上方的倒计时提示**（最后 1 秒变红）。
- **关掉界面重新计时**，不会累积，避免"刚打开就炸"。
- 配置项：`enabled` / `fuse_seconds` / `explosion_power` / `destroy_blocks` /
  `show_countdown` / `include_player_inventory` / `enable_command`。
- 命令：`/craftingbomb on|off|fuse|power|status|reload|selftest`。
- gametest 5 项：
  - 开着工作台计时累加
  - 关掉界面再打开重新计时
  - 玩家背包合成界面默认不计入
  - **引信到点真的爆炸**（用脚下石头被炸掉验证——mock 玩家是创造模式，免疫爆炸伤害，
    所以不能靠掉血验证）
  - 不在工作台界面不计时
- `tools/production-test/`：生产验证数据包，用 `execute store success` 断言自检结果。

### 已知限制

- 只识别原版工作台（`CraftingScreenHandler`），其他模组的合成台不会被计入。
