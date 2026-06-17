# 🎫 Tickets — Phase 2 (vers la beta)

Un ticket par Step de la **Phase 2** de `ROADMAP.md`. Chaque ticket est rédigé pour être
développé **en autonomie complète par un agent IA** : il référence les fichiers réels du dépôt,
les APIs existantes, les contraintes d'architecture, les tests attendus et les critères
d'acceptation.

## Mode d'emploi (agent)
1. Lire `CLAUDE.md` (règles ECS + clean code, non négociables) et la section ciblée de `ROADMAP.md`.
2. Lire **les fichiers listés dans « Contexte / fichiers à lire »** du ticket avant de coder.
3. Implémenter, **compiler** (`/build`), **tester** (`/test`), puis commiter au format `STEP-N: …`.
4. Ne jamais casser un step déjà livré ; chaque ticket doit laisser le projet compilable et lançable.

## Règles transverses (rappel)
- Composants = `record` data-only, **zéro logique**. Systèmes = logique pure, stateless.
- Aucun héritage gameplay. Pas de `HashMap` dans les boucles chaudes (sauf déjà toléré hors hot path).
- Virtual threads réservés à génération / meshing / IO disque.
- Simulation (`simScheduler`, dt fixe 1/60) strictement séparée du rendu (`renderScheduler`, dt variable).
- Pas de `null` dans l'API publique ECS, pas de magic number (→ `WorldConstants`), méthodes < 20 lignes.

## Index des tickets

### Milestone A — Boucle d'interaction complète
- [STEP-13](TICKET-13-real-terrain-generation.md) — Réactiver la vraie génération de terrain
- [STEP-14](TICKET-14-block-highlight.md) — Surbrillance du bloc visé
- [STEP-15](TICKET-15-block-placement.md) — Poser des blocs
- [STEP-16](TICKET-16-inventory-hotbar-data.md) — Inventaire & hotbar (données)
- [STEP-17](TICKET-17-hud-overlay.md) — HUD 2D (crosshair + hotbar)
- [STEP-18](TICKET-18-item-drops-pickup.md) — Drops d'items & ramassage

### Milestone B — Rendu fidèle
- [STEP-19](TICKET-19-texture-atlas.md) — Atlas de textures
- [STEP-20](TICKET-20-water-transparency.md) — Eau & transparence
- [STEP-21](TICKET-21-lighting-engine.md) — Moteur de lumière (skylight + blocklight)
- [STEP-22](TICKET-22-ambient-occlusion.md) — Ambient occlusion (smooth lighting)

### Milestone C — Survie
- [STEP-23](TICKET-23-day-night-cycle.md) — Cycle jour/nuit
- [STEP-24](TICKET-24-health-damage.md) — Vie & dégâts
- [STEP-25](TICKET-25-hunger-food.md) — Faim & nourriture

### Milestone D — Items, craft & blocs fonctionnels
- [STEP-26](TICKET-26-items-tools.md) — Registre d'items & outils
- [STEP-27](TICKET-27-crafting.md) — Craft (2×2 + table 3×3)
- [STEP-28](TICKET-28-furnace-chest.md) — Fourneau & coffre

### Milestone E — Monde vivant (mobs)
- [STEP-29](TICKET-29-entity-framework-rendering.md) — Cadre entités mobiles + rendu
- [STEP-30](TICKET-30-passive-mobs.md) — Mobs passifs + spawn
- [STEP-31](TICKET-31-hostile-mobs-combat.md) — Mobs hostiles + combat + IA

### Milestone F — Simulation dynamique
- [STEP-32](TICKET-32-fluid-simulation.md) — Fluides dynamiques
- [STEP-33](TICKET-33-block-gravity-random-tick.md) — Gravité de blocs & random tick
- [STEP-34](TICKET-34-biomes.md) — Biomes

### Milestone G — Persistance, audio & menus
- [STEP-35](TICKET-35-world-save-load.md) — Sauvegarde / chargement
- [STEP-36](TICKET-36-audio.md) — Audio (OpenAL)
- [STEP-37](TICKET-37-menus-gamemodes.md) — Menus & modes de jeu
