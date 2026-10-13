# compose-multiplatform-kotlin-native TODO

## 已完成（近期，v0.5.68）

- [x] **统一插件 + 单版本 BOM**：`cn.tetraploid` + `cn.tetraploid.app`；BOM 约束宿主/Skiko/UI；消费方主要锁 `composekn.version`
- [x] **稳定 CDN 分发**：Release `composekn-maven-*.zip` → `~/.composekn/m2/<ver>`（脚本 + settings 自动拉取）；Packages 改为 opt-in；Central 发布钩子已预留
- [ ] **发版**：坐标/品牌对齐后需重打 `composekn-maven-0.5.68.zip` 并上传 Release
- [x] **消费冒烟**（0.5.67 基线已绿）：KGP classpath、GMM marker、mingw skia、`run-linux-native`；0.5.68 待新 zip 后再冒烟

## 待做

### 3. `run*` task + 现有 CMP 工程迁移指南

- [x] Gradle task：`runLinuxX64` / `runMingwX64`（host 插件；Linux 优先 `run-linux-native.sh`）
- [x] 文档：[`MIGRATING.md`](MIGRATING.md)
- [x] 与官方 `jvm("desktop")` 并存说明（见迁移文档表格）

### 4. API / 资源对齐 + 能力矩阵

- [x] `compose.resources` 心智对齐：生成/compat 走 `org.jetbrains.compose.resources`；`packageOfResClass` 别名；样本改官方 import
- [x] 公开能力矩阵：[`CAPABILITIES.md`](CAPABILITIES.md)
- [x] 版本对照表：[`CONSUMING.md`](CONSUMING.md)（官方 CMP ↔ 本产品）
