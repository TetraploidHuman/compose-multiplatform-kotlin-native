# compose-multiplatform-kotlin-native TODO

## 命名（已对齐）

| 角色 | 值 |
|------|-----|
| 发布者 | `cn.tetraploid` / `cn.tetraploid.compose`；插件 `cn.tetraploid*` |
| 产品短名 | `composekn`（属性 / 缓存 / 脚本 / `ComposeKN*`） |
| 产品全名 | `compose-multiplatform-kotlin-native` |

## 已完成（近期，v0.5.68）

- [x] 统一插件 + 单版本 BOM；消费方主要锁 `composekn.version`
- [x] Release CDN：`composekn-maven-*.zip` → `~/.composekn/m2/<ver>`
- [x] `runLinuxX64` / `runMingwX64` + [`MIGRATING.md`](MIGRATING.md)
- [x] `compose.resources` 对齐 + [`CAPABILITIES.md`](CAPABILITIES.md) + [`CONSUMING.md`](CONSUMING.md)
- [x] 消费冒烟（0.5.67 基线）

## 待做

- [ ] **发版**：重打并上传 `composekn-maven-0.5.68.zip`（`cn.tetraploid` 坐标），再跑 `./scripts/smoke-composekn-consumer.sh`

入口文档见 [README.md](README.md)。
