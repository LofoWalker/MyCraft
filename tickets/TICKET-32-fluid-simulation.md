# STEP-32 — Fluides dynamiques

**Milestone :** F — Simulation dynamique · **Priorité :** 🟠 Moyenne (step lourd) · **Dépend de :** STEP-20 (rendu eau)

## Objectif
Faire **couler** l'eau (et la lave) : niveaux 0-7, propagation décrémentale via une file d'updates de
blocs, nivellement, et interactions lave+eau → pierre/obsidienne.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/components/VoxelChunkData.java` — stockage des blocs ; le **niveau** de
  fluide doit être encodé (voir spec : métadonnée ou ids dédiés).
- `src/main/java/org/example/systems/ChunkStreamingSystem.java` — `ChunkDirty`/remesh ; les fluides
  marquent les chunks modifiés pour remesh ; meshing eau du STEP-20.
- `src/main/java/org/example/systems/BlockInteractionSystem.java` — la casse/pose génère les évènements
  initiaux de mise à jour de fluide ; util de localisation chunk/cellule.
- `src/main/java/org/example/worldgen/stage/WaterSettleStage.java` — eau statique de la génération ;
  cohérence avec l'eau dynamique.
- `src/main/java/org/example/world/WorldConstants.java` — `BLOCK_WATER`, `WATER_LEVEL` ; ajouter
  `BLOCK_LAVA`, `BLOCK_OBSIDIAN` et constantes de fluide.

## Spécification
1. **Encodage du niveau** : un fluide a un niveau 0..7 (source = niveau « plein »). Choisir une
   représentation cohérente avec `VoxelChunkData` (octet par bloc déjà) : soit des **ids dédiés par
   niveau**, soit une métadonnée parallèle (`byte[]`). Documenter le choix ; rester data-only.
2. **File d'updates** (`FluidSystem`, simScheduler, tické) : ensemble/queue de cellules à réévaluer
   (pas un balayage du monde). À l'évaluation d'une cellule fluide :
   - s'écoule vers le **bas** si la cellule dessous est libre (niveau source en bas) ;
   - sinon s'étale **horizontalement** aux voisins libres avec niveau `-1` (jusqu'à 0) ;
   - une cellule sans voisin source de niveau supérieur s'assèche (retour air).
   Enfiler les voisins modifiés ; marquer les chunks `ChunkDirty`.
3. **Sources** : l'eau de génération / placée à la main agit comme source persistante ; un niveau 0
   isolé disparaît. (Mécanique de source infinie 2-voisins optionnelle, documentée.)
4. **Interactions lave/eau** : lave + eau adjacentes → **obsidienne** (source de lave touchée par eau) ou
   **pierre** (écoulement de lave touché par eau). La lave coule plus lentement (intervalle de tick plus long).
5. **Throttle** : limiter le nombre d'updates de fluide par tick (`MAX_FLUID_UPDATES_PER_TICK`) pour
   borner le coût ; reporter le surplus au tick suivant.
6. **Rendu** : la hauteur de la face d'eau (STEP-20) peut dépendre du niveau (optionnel) ; au minimum, le
   remesh reflète l'apparition/disparition d'eau.

## Contraintes
- **Pas de balayage global** par tick : seulement les cellules en file (et leurs voisins). Structures
  primitives (`long` encodé position) ; éviter `HashMap` dans la boucle d'updates si possible (set d'entiers).
- Mises à jour sur la **simulation** (dt fixe), remesh via `ChunkDirty` côté streaming.
- Pas de magic number : niveaux, intervalles de tick eau/lave, `MAX_FLUID_UPDATES_PER_TICK` → constantes.

## Tests (JUnit 5)
- `FluidSystemTest` (pur, sur une grille de test) : l'eau tombe d'abord, puis s'étale en décrémentant ;
  un niveau isolé s'assèche ; un mur arrête l'écoulement ; nivellement d'une surface.
- `FluidInteractionTest` : lave+eau → obsidienne/pierre selon le cas.
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : casser un mur retenant de l'eau la fait **couler** et se niveler ; placer de l'eau en hauteur
  s'écoule vers le bas puis s'étale ; eau sur lave produit pierre/obsidienne ; pas de freeze.

## Commit
`STEP-32: Add dynamic fluid flow with levels, update queue and lava interactions`
