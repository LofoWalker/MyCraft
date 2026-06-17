# STEP-13 — Réactiver la vraie génération de terrain

**Milestone :** A — Interaction · **Priorité :** 🔴 Haute · **Dépend de :** — (engine livré)

## Objectif
Rebrancher le pipeline de génération « overworld » (terrain ondulé, grottes, eau, arbres, minerais)
à la place du monde plat temporaire, et faire apparaître le joueur sur le sol sans tomber dans le vide.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/worldgen/GenerationPipeline.java` — `overworld(seed)` vs `flat(seed)`.
- `src/main/java/org/example/systems/ChunkStreamingSystem.java` — **ligne 38** : `GenerationPipeline.flat(seed)`.
- `src/main/java/org/example/systems/WorldGenSystem.java` — utilise aussi `.flat(seed)` (non câblé dans `Main`, mais à aligner).
- `src/main/java/org/example/worldgen/stage/` — `TerrainStage`, `CaveStage`, `WaterSettleStage`, `TreeStage`, `OreStage`, `FlatTerrainStage`.
- `src/main/java/org/example/worldgen/TerrainShape.java` et `SurfaceHeights.java` — pour calculer la hauteur de surface au spawn.
- `src/main/java/org/example/Main.java` — **ligne 27** : position de spawn du joueur.
- `src/main/java/org/example/world/WorldConstants.java` — `FLAT_SURFACE_LEVEL`, `TERRAIN_*`.

## Spécification
1. **Pipeline overworld complet** : dans `GenerationPipeline.overworld(seed)`, ajouter `OreStage` à la
   liste (aujourd'hui les minerais ne sont que dans `flat`). Ordre conseillé :
   `TerrainStage → CaveStage → OreStage → WaterSettleStage → TreeStage`.
   Vérifier que `TreeStage` reçoit `BLOCK_GRASS` comme surface cible.
2. **Basculer le streaming** : `ChunkStreamingSystem` (ligne 38) et `WorldGenSystem` doivent appeler
   `GenerationPipeline.overworld(seed)`.
3. **Spawn sûr** : le joueur ne doit plus spawner à `FLAT_SURFACE_LEVEL + 10`. Deux options
   (choisir la plus simple qui marche) :
   - (a) Calculer la hauteur de surface via `TerrainShape`/`SurfaceHeights` en (x=8, z=8) et placer
     le joueur à `surfaceY + 2`.
   - (b) Spawn haut (ex. `WORLD_HEIGHT - 40`) avec gravité : le joueur tombe et la collision le pose.
     ⚠️ Risque : les chunks doivent être chargés avant que le joueur traverse ; préférer (a).
4. **Nettoyage** : `FlatTerrainStage` et `FLAT_SURFACE_LEVEL` peuvent rester pour debug, mais ne
   doivent plus être sur le chemin par défaut. Mettre à jour le commentaire « placeholder » dans
   `WorldConstants` pour refléter que le monde plat n'est plus actif.

## Contraintes
- Aucune régression de perf de streaming : la génération reste sur virtual threads (déjà le cas).
- Pas de magic number pour la marge de spawn → constante `PLAYER_SPAWN_CLEARANCE` dans `WorldConstants`.

## Tests (JUnit 5)
- `GenerationPipelineTest` : un chunk généré via `overworld(seed)` contient de la pierre en
  profondeur, une couche d'herbe en surface, et au moins un bloc d'air au-dessus de la surface
  (déterministe pour un seed fixe).
- Vérifier qu'un seed donné est reproductible (deux générations identiques).
- Lancer toute la suite existante (`/test`) sans régression.

## Critères d'acceptation
- `/run` : le joueur apparaît **debout sur le terrain** (pas de chute infinie, pas enterré).
- Terrain ondulé visible avec arbres ; minerais fer/diamant présents sous terre ; eau dans les creux.
- Aucun chunk ne reste non généré/non meshé autour du joueur.

## Commit
`STEP-13: Re-enable overworld terrain generation and safe player spawn`
