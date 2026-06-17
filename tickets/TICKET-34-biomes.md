# STEP-34 — Biomes

**Milestone :** F — Simulation dynamique · **Priorité :** 🟠 Moyenne · **Dépend de :** STEP-13 (overworld), STEP-19 (textures)

## Objectif
Introduire des biomes pilotés par des cartes **température/humidité** (bruit basse fréquence) qui
déterminent la palette de blocs, la densité d'arbres et la teinte d'herbe (plaine, désert, forêt,
montagne, océan).

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/worldgen/TerrainShape.java`, `SurfaceHeights.java` — hauteur de surface ;
  les biomes s'y greffent sans casser le relief.
- `src/main/java/org/example/worldgen/noise/PerlinNoise.java` — bruit dispo ; cartes T/H = basse fréquence.
- `src/main/java/org/example/worldgen/stage/TerrainStage.java` — pose pierre/terre/herbe en surface ;
  point d'insertion de la palette par biome.
- `src/main/java/org/example/worldgen/stage/TreeStage.java` — densité d'arbres (`TREE_RARITY`) à moduler
  par biome ; surface cible.
- `src/main/java/org/example/worldgen/GenerationPipeline.java` — ordre des stages.
- `src/main/java/org/example/world/BlockType.java` — teinte d'herbe (la teinte de vertex / colorTop) ;
  ajouter `BLOCK_SAND` si pas déjà fait (STEP-33).
- `src/main/java/org/example/world/WorldConstants.java` — échelles de bruit ; ajouter celles des biomes.

## Spécification
1. **`worldgen/BiomeMap.java`** (pur, testé) : `biomeAt(worldX, worldZ) -> Biome` à partir de deux bruits
   basse fréquence (température, humidité) seedés. `Biome` (enum) : PLAINS, DESERT, FOREST, MOUNTAINS,
   OCEAN (+ éventuellement BEACH). Table T/H → biome.
2. **Palette par biome** : `TerrainStage` consulte `BiomeMap` pour la surface :
   - DESERT : sable en surface + grès/sable sous la couche, **pas d'herbe** ;
   - PLAINS/FOREST : herbe + terre ;
   - MOUNTAINS : pierre exposée au-dessus de `ROCK_LEVEL` ;
   - OCEAN : bassin profond (relief abaissé) rempli d'eau.
3. **Relief par biome** : moduler l'amplitude/le niveau de base de `TerrainShape` selon le biome
   (montagnes plus hautes, océans creusés, plaines plates), avec **interpolation** entre biomes voisins
   pour éviter les murs nets à la frontière.
4. **Arbres par biome** : `TreeStage` module `TREE_RARITY` (forêt dense, plaine clairsemée, désert/océan
   nuls) via `BiomeMap`.
5. **Teinte d'herbe** : la couleur/teinte d'herbe (attribut de teinte du meshing STEP-19) varie par biome
   (plaine vive, forêt foncée). Exposer `grassTint(Biome)`.
6. **Câblage** : `BiomeMap` partagé (construit depuis le seed) injecté dans les stages concernés via
   `GenerationPipeline.overworld(seed)`.

## Contraintes
- Génération **déterministe** par seed ; transitions de biomes interpolées (pas de discontinuité de
  hauteur brutale).
- Pas de magic number : échelles de bruit T/H, seuils de table de biome, amplitudes par biome → constantes.
- `BiomeMap` et palettes **purs** (pas de World/GL), testables hors contexte.

## Tests (JUnit 5)
- `BiomeMapTest` : déterminisme ; couvre plusieurs biomes sur une plage de coordonnées ; T/H connus → biome attendu.
- `TerrainStageTest` (extension) : un désert pose du sable et aucune herbe ; un océan se remplit d'eau ;
  une plaine pose herbe+terre.
- `TreeStageTest` (extension) : densité d'arbres plus élevée en forêt qu'en plaine, nulle en désert.
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : transitions de biomes visibles (déserts de sable sans herbe, forêts denses, montagnes
  rocheuses, océans) ; teinte d'herbe variable ; pas de frontières en falaise brutale.

## Commit
`STEP-34: Add temperature/humidity biomes with per-biome palette and trees`
