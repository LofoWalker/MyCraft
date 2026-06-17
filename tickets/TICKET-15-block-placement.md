# STEP-15 — Poser des blocs

**Milestone :** A — Interaction · **Priorité :** 🔴 Haute · **Dépend de :** STEP-14

## Objectif
Permettre de poser un bloc (clic droit) sur la face du bloc visé, sans poser dans le joueur.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/components/PlayerInput.java` — record d'input à étendre.
- `src/main/java/org/example/systems/InputSystem.java` — capture clavier/souris, écrit `PlayerInput`.
- `src/main/java/org/example/systems/BlockInteractionSystem.java` — détection de clic edge-triggered,
  écriture voxel + `ChunkDirty` (modèle pour la pose).
- `world/VoxelRaycast.java` (créé au STEP-14) — fournit la normale de face (`faceX/Y/Z`).
- `src/main/java/org/example/components/ColliderAABB.java`, `Position.java` — pour le test anti-collision.
- `src/main/java/org/example/systems/ChunkStreamingSystem.java` — remeshing via `ChunkDirty` (déjà géré).

## Spécification
1. **Input** : ajouter `boolean placeBlock` à `PlayerInput` (clic droit `GLFW_MOUSE_BUTTON_RIGHT`).
   Mettre à jour toutes les constructions du record (`Main`, `InputSystem`, tests).
2. **Pose** : dans `BlockInteractionSystem` (ou un `BlockPlacementSystem` dédié), edge-trigger sur
   `placeBlock` :
   - Raycast (via `VoxelRaycast`) → `RaycastHit`.
   - Cellule cible = `(hit.x + faceX, hit.y + faceY, hit.z + faceZ)`.
   - **Refuser** si la cellule n'est pas de l'air, ou si l'AABB du joueur intersecte cette cellule
     (sinon on s'enferme). Test d'intersection AABB vs cellule [c, c+1].
   - Sinon `VoxelChunkData.set(...)` du bloc choisi + ajouter `ChunkDirty` sur le chunk
     (gérer le cas où la cellule tombe dans un chunk voisin).
3. **Bloc à poser** : pour ce step, une constante (ex. `BLOCK_STONE`) suffit ; au STEP-16 ce sera
   l'item sélectionné dans la hotbar. Garder le choix isolé (méthode `selectedBlock(world, player)`)
   pour brancher la hotbar ensuite sans refactor.
4. **Mur de chunk** : factoriser le calcul (chunk d'un bloc monde, indices locaux, `ChunkDirty`) dans
   un util réutilisé par casse **et** pose (éviter la duplication actuelle dans `removeBlock`).

## Contraintes
- Edge-trigger : un clic droit maintenu ne pose pas un bloc par frame.
- Pas de pose hors des chunks chargés (no-op si chunk absent).

## Tests (JUnit 5)
- Test d'intersection AABB joueur vs cellule (pose refusée quand le joueur occupe la cellule).
- Test logique « cellule adjacente » : pour un hit + normale donnés, la cellule visée est correcte.
- Test : poser dans une cellule non-air est refusé.

## Critères d'acceptation
- `/run` : clic gauche casse, clic droit pose sur la face visée ; impossible de se murer dans soi-même ;
  le mesh se met à jour immédiatement (chunk courant et voisin).

## Commit
`STEP-15: Add block placement on targeted face with player-collision guard`
