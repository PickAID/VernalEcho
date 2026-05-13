# Changelog

## 0.1.0

- Enable Curios in the workspace feature flags and local development pack for the Teacon design track.
- Add Sodium and Iris as normal deobfuscated compile/runtime dependencies for future client shader compatibility code and runClient checks.
- Rewrite the public design document around the Teacon SoulWorkBlock block entity, configurable Echo blueprints, skin/profile support, Echo entity rendering, tooltip polish, and temporary natural-generation suspension.
- Clarify Echo blueprint item boundaries: save equipment templates, avoid full inventory snapshots, and use bounded loadout/item pools for switching behavior.
- Refine Echo AI boundaries: item libraries require owner-inventory matches, food heals Echo health with cooldown, live Echoes can be recalled, killed Echoes die, and attributes use profile ids plus sparse overrides instead of full per-blueprint dumps.
- Refine Echo item-use design around loadout views, held-item access, use traits, predicates, action registry, one-shot/channelled actions, projectile plans, and planner-driven hand switching.
- Separate static preview poses from moving Echo entities, switch attributes to lightweight profiles plus sparse overrides, and remove non-vanilla spell-book defaults from core design.
- Initial Vernal Echo design and template migration for versioned platform uploads.
