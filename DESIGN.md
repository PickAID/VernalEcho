# Vernal Echo 设计文档

## 当前构建目标

- Minecraft / NeoForge 线：`26.1.2`
- 已解析 NeoForge artifact：`net.neoforged:neoforge:26.1.2.30-beta`
- Loader：NeoForge
- Java toolchain：JDK `25`
- 已验证字节码：class major version `69`
- 主配置文件：`project.toml`
- Mod id：`vernalecho`
- 包根路径：`org.pickaid.vernalecho`

Gradle 自身仍可能用 Java 17 启动 daemon。项目编译使用 `gradle/template-defaults.toml` 配置的 Java 25 toolchain。

## 设计基准

本设计基于当前 Gradle 解析到的 `26.1.2` 源码和工作区源码。代码与设计都以 `org.pickaid.vernalecho` 为唯一包根路径，不沿用旧版包名或 1.21.1 假设。

已确认的本地源码基准：

- patched Minecraft source：`build/moddev/artifacts/minecraft-patched-26.1.2.30-beta-sources.jar`
- NeoForge source：`neoforge-26.1.2.30-beta-sources.jar`
- Curios：`top.theillusivec4.curios:curios-neoforge:15.0.0-beta.2+26.1.2`

重要 API 事实：

- Minecraft resource id 使用 `net.minecraft.resources.Identifier`。
- 区块本地数据优先使用 NeoForge data attachment。
- `AttachmentType<T>` 注册到 `NeoForgeRegistries.Keys#ATTACHMENT_TYPES`。
- 修改 `ChunkAccess` attachment 后调用 `ChunkAccess#markUnsaved`。
- 持久化数据应优先用 `Codec`，不要写新旧版裸 NBT serializer 假设。

## Teacon 特供版本目标

Teacon 特供版本是一条“封印自然生态，开放人工 Echo 锚点”的展示线。它暂时搁置自然生成，把核心体验集中到 `EchoAnchorBlock`、可配置 Echo 数据、自然 Echo 同款投影渲染、假人式 Echo 实体、皮肤/Profile 基础设施、物品 tooltip 和 Curios 扩展准备上。

玩家看到的体验应当是：

```text
EchoAnchorBlockEntity 保存可编辑 EchoPattern
-> 有权限的玩家配置身份、皮肤、姿态、装备、可用物品和属性轮廓
-> 方块向客户端提供和自然 Echo 相同形状的 EchoProjection
-> 玩家可无限收取该投影产出的 Echo
-> Bell、tooltip、锚点投影和 Echo 实体使用同一套身份与外观语义
```

Teacon 中，Echo 不应因为世界生成、玩家跨 chunk、旧区块回填或活动积累而自然出现。

## 自然生成封印

当前工程中 Wild Echo 已有 worldgen 和 chunk-entry 回填路径。Teacon 版本必须把这些路径全部 gate 掉，使它们在游戏内不生效。

需要封印的入口：

- `VernalEcho.java` 中的 `EchoFeatures.register(modBus)`。
- `VernalEchoDataGenerators` / `VernalEchoWorldgen` 中的 Wild Echo 数据生成。
- `EchoActivityHandler` 中的 `EntityEvent.EnteringSection` 回填入口。
- `EchoNaturalSpawner` 中 feature placement 和旧区块 backfill 调用。
- 已生成的 Wild Echo biome modifier、configured feature 和 placed feature JSON。

验收标准：

- `runData` 后不输出 Wild Echo worldgen JSON。
- 新世界不会自然生成 Wild Echo。
- 玩家跨 chunk 不触发 Echo 回填。
- 旧世界不会因为加载或移动补出 Echo。

## 统一来源：EchoSource

Echo 的世界表现先统一成来源模型，再分自然生成和人工锚点。自然生成 Echo 与 Teacon 方块 Echo 不应分成两套渲染或两套数据语义；它们都提供一个可渲染、可交互、可被 Bell 收取的 Echo 投影视图。

核心类型：

- `EchoSource`：服务端 Echo 来源接口。
- `EchoSourceType`：`NATURAL`、`ANCHOR`，未来可扩展 `STRUCTURE`、`COMMAND`、`SCRIPTED`。
- `EchoRecord`：自然生成和活动积累产生的有限 Echo 数据。
- `EchoPattern`：`EchoAnchorBlockEntity` 内保存的可编辑 Echo 模板。
- `EchoProjection`：客户端和交互逻辑使用的只读投影视图，由 `EchoRecord` 或 `EchoPattern` 生成。
- `EchoProjectionProvider`：把来源转换为投影视图的 provider。
- `EchoProjectionRenderer`：自然 Echo 和锚点 Echo 共用的投影 renderer。
- `EchoBodyType`：投影身体类型，第一版支持 `PLAYER`，未来支持其他生物 Echo。

来源差异：

```text
NaturalEchoSource
- 来源数据：EchoRecord
- 生命周期：有限，成熟后可收取并移除
- 编辑权限：无
- 渲染：EchoProjectionRenderer

EchoAnchorSource
- 来源数据：EchoPattern
- 生命周期：无限，收取不移除
- 编辑权限：权限门控
- 渲染：EchoProjectionRenderer
```

Teacon 中，`EchoAnchorSource` 是主要入口；自然生成相关入口仍然封印，但其数据形状和渲染路径不能废弃，因为锚点 Echo 需要和自然 Echo 看起来一致。

成熟规则必须由来源决定：

- `NATURAL` / Wild Echo：创建时即成熟，`formationProgress = 1.0`。
- `PLAYER_ACTIVITY`：由玩家活动产生，需要按时间成熟。
- `ANCHOR`：Teacon 锚点投影默认成熟，除非 Pattern 显式配置展示未成熟状态。

非玩家 Echo 预留：

- 不把 Echo 写死成 player model；`EchoProjection` 应携带 `bodyType`、`entityType` 或等价 model key。
- `PLAYER` 类型使用 player-like 模型、profile/skinSource 和装备槽。
- 其他生物类型使用对应实体模型语义、缩放、姿态集合和可用装备边界；没有装备槽的生物不强行套玩家装备。
- `EchoPattern` 和 Bell 存的是稳定的类型 id 与外观 descriptor，不存运行时 renderer 对象。

## EchoAnchorBlock：可编辑 Echo 锚点

`EchoAnchorBlock` 是 Teacon 的主展示方块。它必须是 `BlockEntity`，不是无状态右键方块。它不是单独的工作台系统，而是一个可配置、无限产出的 Echo 来源。

幻想语义：

- 方块像把 Echo 锚在当前位置的青蓝结晶，外观不需要解释成机器。
- 方块上方或方块前方渲染的 Echo 投影，必须和自然生成 Echo 使用同一套姿态、道具剪影和半透明材质。
- 它的特殊点不是“长得不同”，而是 `infinite` 和 `editable`：可无限收取，并允许有权限的玩家编辑 Echo 信息。

工程职责：

- `EchoAnchorBlock`：方块交互入口。
- `EchoAnchorBlockEntity`：保存 `EchoPattern`、产出规则、权限状态和同步状态。
- `EchoPattern`：可序列化的 Echo 模板，不是实体快照。
- `EchoAnchorMenu` / `EchoAnchorScreen`：服务端容器和客户端配置界面。
- `EchoProjectionProvider`：把 `EchoPattern` 转成 `EchoProjection`。
- `EchoProjectionRenderer`：复用自然 Echo 投影渲染。

建议包结构：

```text
org.pickaid.vernalecho.echo.anchor
org.pickaid.vernalecho.echo.client.projection
```

## EchoPattern 数据

`EchoAnchorBlockEntity` 保存一份 `EchoPattern`。Pattern 是人工 Echo 锚点的低成本配置来源，不是完整实体快照，也不是玩家背包的复制容器。

建议字段：

```text
EchoPattern
- schemaVersion
- echoId
- displayName
- echoProfile
- skinSource
- echoForm
- displayPose
- maturity
- equipmentSet
- usableItems
- inventoryPredicates
- attributeProfile
- attributeOverrides
- echoOutput
- isSealed
- permissionMode
```

身份字段：

- `echoId`：允许手动设置；为空时生成稳定 UUID 字符串。
- `displayName`：用于 UI、tooltip 和实体名牌。
- `profileName` / `profileUuid`：用于离线玩家信息和皮肤解析。
- `displayPose`：只用于 tooltip、锚点投影和静态残影，不用于会移动的 Echo 实体。

成熟规则：

- Teacon 中由 `EchoAnchorBlock` 提供的 Echo 投影默认成熟。
- 未来 Wild Echo 的自然成熟逻辑保留，但 Teacon 不调用自然生成路径。

锚点规则：

- `EchoAnchorBlockEntity` 只保存 `EchoPattern` 和少量方块状态。
- 客户端渲染不直接读 BE 私有字段；服务端同步或客户端缓存一份 `EchoProjection`。
- 收取锚点 Echo 不删除 `EchoPattern`，也不把 `isSealed` 改成 false。
- 只有有权限的玩家可以修改 `EchoPattern`；普通玩家只能观察和收取。

## 皮肤与离线玩家信息

皮肤配置应存为 descriptor，而不是把下载和渲染绑在一起。

支持模式：

- `DEFAULT`：使用默认青蓝 Echo 轮廓。
- `PROFILE`：通过 profile name/uuid 走 Minecraft 原生 profile 与 skin manager 路线。
- `URL`：保存 URL descriptor，客户端异步缓存解析；渲染线程不下载。
- `RANDOM_POOL`：从本地预设 profile/skin 池中选择。
- `CUSTOM_SKIN_LOADER_COMPAT`：保持原生 profile/skin manager 路线，让 CustomSkinLoader 有机会接管。

离线玩家规则：

- 显式 UUID 优先。
- 只有名称时，允许离线 UUID fallback。
- 解析失败时回退到默认 Echo 轮廓。
- Echo 不应伪装成在线玩家加入 player list，除非未来单独实现完整假玩家功能。

## 装备、道具和 Attribute

装备配置是 `EchoPattern` 的一等能力。

第一版槽位：

- 主手
- 副手
- 头盔
- 胸甲
- 护腿
- 靴子
- 预留 Curios 槽位

装备用途：

- 参与实体 Attribute 应用和 AI 决策。
- 影响 tooltip、锚点投影和实体 renderer 的道具影子。
- 不作为掉落物复制给玩家。

## Pattern 物品边界

`EchoPattern` 必须保存武器和护甲模板，但不应保存完整玩家背包。

保存范围：

- `equipmentSet`：当前主手、副手、四件护甲和预留 Curios 槽。
- `usableItems`：固定可配置的允许物品集合，用于定义 Echo 可以尝试使用哪些 `ItemStack` 模板和匹配规则。核心不内置非原版物品分类。
- `inventoryPredicates`：从召唤者背包中匹配 `usableItems` 条目的规则。
- `renderedProps`：只影响渲染的道具覆盖项。

不保存范围：

- 玩家完整背包。
- 末影箱、饰品模组的完整库存、背包类物品内部库存。
- 可被玩家从蓝图中取回的真实物品库存。

边界理由：

- Echo 需要主副手和护甲来应用 Attribute、展示道具和渲染装备。
- 切换物品不需要完整背包；固定物品库加召唤者背包匹配足够支持 Teacon 展示和未来 AI。
- 完整背包会把蓝图变成物品复制容器，也会扩大同步和存档体积。

安全规则：

- Pattern 中的 `ItemStack` 是模板，不是库存。
- Echo 只有在召唤者背包中找到匹配物品时，才可以临时使用对应模板。
- Echo 使用的是临时视图或副本，死亡、移除、导出、收取和收回都不掉落这些物品。
- EchoAnchor UI 可以从玩家当前装备复制模板，但不能从 Pattern 把模板取回玩家背包。
- 若未来需要“记忆背包”，应设计为独立的 `EchoMemoryInventory`，默认最多 9 个非提取模板槽，并且不等同于玩家背包。

## Attribute 配置

`EchoPattern` 不维护 `echoPower`、`equipmentScore` 这类自定义强度字段，也不在每个 Pattern 里长期保存完整 Attribute 实例快照。Pattern 保存轻量的 Attribute profile 引用和稀疏 overrides；只有生成投影、召唤实体或打开需要完整预览的界面时才解析成运行时 attribute instance。

推荐结构：

```text
EchoAttributeProfile
- id: Identifier
- parent: Optional<Identifier>

EchoAttributeOverrides
- entries: List<SparseAttributeOverride>

SparseAttributeOverride
- attribute: ResourceKey 或 Identifier
- baseValue: double
- modifiers: List<SparseAttributeModifier>

SparseAttributeModifier
- id: Identifier
- amount: double
- operation: ADD_VALUE | ADD_MULTIPLIED_BASE | ADD_MULTIPLIED_TOTAL
```

规则：

- 支持 vanilla 和其他模组注册的 Attribute。
- 默认属性放在可复用 profile 里；单个 Echo 只保存和默认不同的 sparse overrides。
- 读不到的 Attribute 不崩溃，保留 override 的 id，并在 tooltip/UI 中标记为缺失。
- 装备不再通过 `equipmentScore` 合并成一个数值；需要装备影响时，让装备本身或 Echo item-use 逻辑提供 Attribute modifier。
- tooltip 可以摘要显示生命、攻击、护甲、速度等常见 Attribute，但存档层避免为每个 Echo 存重复列表。
- 大量 Echo 场景中，未激活的 Echo 只保存 profile id、少量 overrides 和必要 runtime state。
- 运行时实体可以持有完整 Attribute instance；这个完整列表来自解析结果，不直接塞回蓝图长期保存。

## EchoAnchor 无限产出规则

`EchoAnchorBlock` 可以无限产出 Echo。收取行为不消耗 Pattern，不消耗装备，也不让方块枯竭。

推荐产出字段：

```text
mode: ITEM | PLAYER_RESOURCE | BOTH
echoItemId: Identifier
amountPerUse: int
quality: COMMON | RARE | TEACON
interactionCooldownTicks: int
playFeedback: boolean
```

规则：

- 默认每次产出 `1` 个 Echo item 或等价玩家资源。
- 冷却只防止连续点击刷屏，不限制总产量。
- 背包满时应有明确处理：掉落到方块前方或提示失败，不能静默吞掉。
- 成功后播放青蓝 Echo 粒子、短音效和投影反馈。

## 假人式 Echo 实体

Teacon 需要注册真正的 Echo 实体表面，但第一版不走完整 `ServerPlayer` 假人路线。实体是会移动、受伤、死亡和交互的个体；它不以静态 pose 作为行为模型。

推荐路线：

- 注册 `EntityType<EchoEntity>`。
- `EchoEntity` 持有从 `EchoPattern` 或收取结果解析出的运行时配置。
- 实体从 Pattern 应用 Attribute profile/overrides、装备、行为配置、显示名和皮肤 descriptor。
- 客户端用 player-like 模型渲染青蓝半透明 Echo。

职责拆分：

- `EchoEntity`：服务端实体状态、生命、死亡、交互和收回。
- `EchoProfile`：身份、离线信息、皮肤来源。
- `EchoAppearance`：模型、透明度、颜色和粒子。
- `EchoLoadout`：装备与 prop plan。
- `EchoAttributeResolver`：profile + sparse overrides 的解析、物化和缺失 Attribute 兼容。
- `EchoActionState`：移动、攻击、格挡、使用物品和 addon 动作等运行时状态。
- `EchoAnimationState`：客户端从动作状态推导出的动画状态，不进蓝图长期存档。
- `EchoSkinResolver`：客户端皮肤解析。
- `EchoEntityRenderer`：渲染入口。

持久化边界：

- Bell 或方块只保存 pattern id、profile/skin descriptor、equipmentSet 模板、attribute profile/overrides 和少量运行时状态。
- 被召回的 Echo 只额外保存剩余血量、关键冷却和必要 action state，不保存完整路径、目标缓存、AI 黑板或 renderer 状态。
- 需要重建的内容在放出时重新解析：Attribute instance、装备 modifier、皮肤缓存、动画状态和 item-use planner。

生命周期规则：

- Echo 有真实血量，最大生命值来自解析后的 Attribute profile + overrides。
- Echo 被杀死就直接死亡，不能收回到铃铛，也不掉落模板装备或物品库物品。
- Echo 未死亡时可以被收回到 Echo Bell；收回应保存剩余血量、冷却和必要的运行时状态。
- 再次放出时可以延续这些运行时状态，或由 Bell 明确执行重置，具体在实现计划中固定。

## Echo AI 与物品使用

Echo 的 AI 必须可定义、可扩展，不能把特定物品类别和模组道具都写死在 `EchoEntity` 里。核心只认识“能否匹配到物品、当前上下文是否允许使用、使用动作由谁处理”。

推荐拆分：

- `EchoInventoryView`：只读查看召唤者背包、主副手和可选 Curios 槽。
- `EchoUsableItems`：Pattern 配置的固定可用物品集合。
- `EchoItemMatcher`：判断召唤者是否携带可用物品集合中的匹配物。
- `EchoUseBehavior`：可注册的物品使用行为。
- `EchoUseContext`：包含 Echo、召唤者、目标、手、世界、只读属性视图和冷却信息。
- `EchoUseSelector`：按 AI 状态选择下一次物品使用。

物品动作抽象：

- `EchoEquipmentView` 暴露当前主副手模板、装备模板和远程/近战模式切换状态。
- `EchoItemAccess` 提供只读或受控写入的 `ItemStack` 视图，避免 Echo 直接拥有可提取库存。
- `EchoItemUseProperties` 描述物品能力：近战、远程、盾牌、防御性、优先级和是否允许 fallback。
- `EchoUsePredicate` 根据 Echo、物品、手和目标判断物品能否成为当前动作来源。
- `EchoUseBehaviorRegistry` 按 priority 查询 behavior；找不到匹配项时只回退到安全的 vanilla right-click 或空手逻辑。
- `InstantEchoUseBehavior` 处理一次性触发道具，声明 range、cooldown、消耗和副作用。
- `ChargedEchoUseBehavior` 处理需要拉弓、蓄力、持续施法或持续使用的道具，声明 chargeTime、tickUsing、trigger 和是否允许期间近战。
- `EchoProjectileContext` 负责弹药选择、无限弹药判定、弹道估算、发射位置、初速、重力、散布和多发角度。
- `EchoUseSelector` 负责目标距离、冷却、优先级、是否可近战、主副手切换和失败回退；`EchoEntity` 不把这些规则写死在 tick 里。

物品库规则：

- `usableItems` 表示 Echo 允许尝试使用的固定物品集合。
- Echo 每次使用前都必须通过 `EchoItemMatcher` 在召唤者背包中找到匹配物。
- 找不到匹配物时，Echo 只能显示蓝色道具影子或回退到空手逻辑，不能凭空生成物品效果。
- 匹配物默认不消耗召唤者物品；需要消耗的动作必须由具体 `EchoUseBehavior` 明确声明并在服务端执行。

食物规则：

- 第一版建议食物直接恢复 Echo 血量，而不是引入玩家饥饿值。
- 原因是 Echo 不是完整玩家，饥饿值会引入饱食度、消耗、自然回血和 UI 状态，复杂度超过 Teacon 需要。
- 食物使用有独立冷却，例如 `foodCooldownTicks`。
- 未来如果某类 Echo 需要饥饿、魔力或其他资源，把它做成 `EchoResourceModel` 插件，不放进基础 Echo。

右键物品规则：

- 非食物道具走 `EchoUseBehavior`，默认语义尽量接近玩家右键。
- 使用时按需读取“召唤者当时状态”：关键 Attribute 视图、队伍/阵营、权限上下文、朝向、目标和 addon 暴露的资源接口。不要把这些内容长期保存进蓝图。
- 对复杂模组物品，例如铁魔法类施法道具，核心只提供通用 use context；具体兼容由 addon 或 adapter 注册 `EchoUseBehavior`。
- 无适配器的未知物品不强行调用危险逻辑，只显示道具影子或执行安全 fallback。

## Shader 兼容验证

Sodium 和 Iris 作为普通 dependencies 加入本地开发环境：compile-only 让后续 compat 代码可以引用必要 API，runtime-only 让 `runClient` 能加载实际模组验证 Echo 半透明剪影、tooltip 预览、beam/fx pipeline 和 shader pack 下的渲染行为。

边界：

- 不在核心逻辑中直接依赖 Sodium/Iris；compat 代码应放到独立 adapter 或隔离包里。
- 兼容修复先通过渲染状态、RenderType、buffer ownership 和资源 reload 边界完成。
- 只有当必须接入特定 renderer hook 时，才在 compat adapter 中引用 Sodium/Iris API。

## 静态剪影与动态实体渲染

当前工程已有 `EchoPoseRenderers`、`EchoPoseRenderContext` 和 `EchoPropRenderPlan`。这些概念只适合静态投影：tooltip、EchoAnchor 投影、未激活残影、宝箱前蹲下伸手这类不会真的行动的视觉。会移动、会攻击、会吃东西、会右键物品的 Echo 实体不应从 Pattern 读取静态 pose。

目标：

- tooltip、EchoAnchor 投影和静态残影共用 pose 逻辑。
- 实体 renderer 读取 `EchoActionState` / `EchoAnimationState`，不读取 `displayPose`。
- tooltip renderer 只负责画布、相机和光照。
- pose definition 负责模型姿态和 prop 收集。
- prop renderer 负责物品、盾牌、书本和 addon 注册道具的 Echo 化渲染。

静态预览姿态：

- `IDLE`
- `WALKING`
- `CROUCHING`
- `REACHING`
- `CROUCH_REACHING`
- `GUARDING`
- `READING`
- `FALLEN`

`CROUCH_REACHING` 保留“宝箱前蹲下伸手”的展示语义，但 Teacon 不通过自然生成把它放到宝箱前。未来如果恢复自然结构联动，它也应先生成静态记录或预览记录，不直接把 live entity 固定成这个姿态。

实体动画边界：

- 实体动画由移动速度、攻击目标、手上动作、受击、使用物品和 AI 状态推导。
- 实体可以在客户端做插值和动作 blending，但服务器只同步必要动作状态。
- 不把静态 pose enum 存进实体长期数据。
- 插件需要新动画时注册 action/animation adapter；插件需要新静态展示时注册 projection pose definition。两者分开维护。

## Echo Bell 与 Tooltip

Echo Bell 应像一个小型 Echo 投影容器。

要求：

- Bell 可保存 `EchoPattern` 快照或收取结果。
- Bell 可从 `EchoAnchorBlockEntity` 导出 Pattern。
- Bell 可把 Pattern 导入 `EchoAnchorBlockEntity`。
- Bell 物品模型应做成 3D 模型，而不是只依赖平面 item texture。
- Bell 被 Curios 装备时，应在玩家右肩附近渲染同一个 3D 模型；这属于客户端饰品渲染，不改变 Bell 的服务端数据。
- 附近存在可收取 Echo 投影时，装备中的 Bell 可以发出一次提示音；提示必须有 per-echo 或 per-source cooldown，避免玩家站在 Echo 旁边持续响。
- tooltip 显示 Echo ID、显示名、Attribute 摘要、皮肤模式、成熟状态和静态预览姿态。
- tooltip 渲染装备与道具影子。
- 移除 tooltip renderer 中的占位 texture label。
- 文案使用 `zh_cn.json` 和 `en_us.json` 本地化。

收取/放出一致性：

- Bell 不应只保存 `recordId + pos` 作为长期所有权依据；目标锁定可以临时保存位置，但提交收取时必须验证 `sourceId`、`recordId`、`origin` 和当前位置仍一致。
- 收取有限 Echo 时，服务端必须在同一次事务中从来源移除 Echo 并写入 Bell；失败时不能只完成一边。
- 放出有限 Echo 时，应生成新的世界实例 id 或明确迁移 owner/source state，不能简单复用旧 record id 在新位置复制一份。
- 放出后 Bell 清空与 chunk/BE 写入必须同一服务端操作完成；chunk 或 BE 需要 `markUnsaved` 或等价脏标记。
- 如果游戏强制关闭，重进世界时不能出现“Bell 里有 Echo，世界 A 点又恢复同一个 Echo”的双拥有状态。
- 对 `NATURAL` 来源，收取后该 record 应进入 consumed 状态或从来源中移除；自然 backfill 不能因为 slot 状态残留或 id 复用再次生成同一个 Echo。
- 对 `ANCHOR` 来源，收取不删除 Pattern，但 Bell 中保存的是一次产出的 Echo 结果，不获得 Anchor Pattern 的所有权。

## Curios 集成

Curios 已在 `project.toml` 中开启。Teacon 第一版把 Curios 作为扩展面，而不是核心运行前提。

使用策略：

- EchoPattern 预留 Curios 槽位结构。
- Echo Bell 可以作为 Curios 饰品装备；装备后客户端在玩家右肩渲染 Bell 3D 模型。
- 饰品渲染只读 Bell item stack 和 player pose，不写 Echo 数据。
- 真正添加 Echo 饰品时，用 Curios datagen 生成 slot、entity assignment 和 item tag。
- 没有 Curios 玩法内容时，不创建空洞 UI。

可能的后续物品：

- `echo_charm`：增强 Echo 收取反馈或数量。
- `profile_locket`：保存 Echo 身份或皮肤 descriptor。
- `teacon_relic`：展会调试用的便携入口。

## Datagen 策略

长期维护的数据包和资源包内容必须由 datagen 生成。

Teacon 应生成：

- `EchoAnchorBlock` blockstate、block model、item model。
- Echo Bell 3D item model、手持显示 transform 和 Curios 肩部渲染所需模型资源。
- Echo item 或 Pattern item model。
- `zh_cn.json` 和 `en_us.json` 文案。
- Echo 实体 spawn egg 或展示物品，如果实现。
- Curios slot/tag 数据，如果实现对应内容。

Teacon 不应生成：

- Wild Echo biome modifier。
- Wild Echo configured feature。
- Wild Echo placed feature。
- 任何自然生成入口数据。

## 实现顺序

1. 建立 Teacon build flags，封印 worldgen、datagen 和 EnteringSection 回填。
2. 提取 `EchoProjection` / `EchoProjectionRenderer`，让自然 Echo 和锚点 Echo 共用投影渲染。
3. 注册 `EchoAnchorBlock`、`EchoAnchorBlockEntity`、基础模型和语言 datagen。
4. 实现 `EchoPattern`、skin descriptor、equipmentSet、usableItems、attribute profile/overrides 和 echo output。
5. 修复 Bell 收取/放出的一致性：所有权迁移、id 策略、chunk/BE 脏标记、强退恢复和自然 backfill 去重。
6. 实现权限门控的锚点编辑 UI 与无限收取交互。
7. 实现 Bell 3D 模型、手持显示和 Curios 右肩挂载渲染。
8. 实现附近 Echo 一次性提示音，带 per-source cooldown。
9. 实现 Bell 与 EchoAnchorBlock 的 Pattern 导入/导出。
10. 注册 `EchoEntity`，从 Pattern 或 Bell 收取结果应用 Attribute profile、装备、行为配置和成熟状态；实体不读取 `displayPose` 驱动行为。
11. 实现 Echo AI 的 usableItems、inventory matcher、use behavior 和冷却框架，并保留 addon behavior adapter 入口。
12. 整理静态 pose/prop registry，复用到 tooltip、EchoAnchor 投影和静态残影。
13. 实现 profile、随机皮肤、URL descriptor 和 CustomSkinLoader 兼容路径。
14. 为非玩家 Echo 建立 `EchoBodyType` / model key / renderer adapter 边界。
15. 精修 tooltip、中文文案、展示反馈和 changelog。
