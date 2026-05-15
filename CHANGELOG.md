# Changelog

## 0.1.0

- Enable Curios in the workspace feature flags and local development pack for the Teacon design track.
- Add Sodium and Iris as normal deobfuscated compile/runtime dependencies for future client shader compatibility code and runClient checks.
- Rewrite the public design document around `EchoSource`, `EchoProjection`, and the Teacon `EchoAnchorBlock` block entity, with temporary natural-generation suspension.
- Clarify that EchoAnchor is an infinite, permission-editable Echo source that renders through the same projection path as natural Echo records.
- Add design requirements for a 3D Echo Bell model, Curios right-shoulder Bell rendering, and one-shot nearby Echo notification sound.
- Clarify maturity rules: natural Echoes are mature at creation, player-activity Echoes mature over time, and EchoAnchor projections are mature by default.
- Clarify finite source rules: natural and player-activity Echoes are consumed when extracted, while only EchoAnchor remains as an infinite source.
- Document the Bell collect/release consistency fix: Echo ownership must transfer atomically between source, Bell, and world placement to prevent duplicate A/B-point records after reloads or repeated moves.
- Reserve `EchoBodyType` / model-key boundaries so future Echoes can represent non-player creatures without forcing player-model data onto every projection.
- Clarify EchoPattern item boundaries: save equipment templates, avoid full inventory snapshots, and use bounded usable item sets plus inventory predicates for switching behavior.
- Refine Echo AI boundaries: usable items require owner-inventory matches, food heals Echo health with cooldown, live Echoes can be recalled, killed Echoes die, and attributes use profile ids plus sparse overrides instead of full per-pattern dumps.
- Refine Echo item-use design around equipment views, item access, use properties, predicates, behavior registry, instant/charged behaviors, projectile contexts, and selector-driven hand switching.
- Separate static display poses from moving Echo entities, switch attributes to lightweight profiles plus sparse overrides, and remove non-vanilla spell-book defaults from core design.
- Initial Vernal Echo design and template migration for versioned platform uploads.
