# STEP-14 — Surbrillance du bloc visé

**Milestone :** A — Interaction · **Priorité :** 🔴 Haute · **Dépend de :** STEP-13

## Objectif
Afficher un contour (wireframe) sur le bloc que le joueur regarde, dans la portée d'interaction.
Pré-requis ergonomique pour poser/casser avec précision.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/BlockInteractionSystem.java` — contient `raycastSolid(...)`
  (algorithme Amanatides & Woo) et la construction du vecteur de direction depuis `Rotation`.
- `src/main/java/org/example/systems/BlockBreakOverlaySystem.java` — **modèle à copier** : système de
  rendu qui dessine un cube sur un bloc via `Shader` + `Mesh.createTestCube()`, depth/blend gérés.
- `src/main/java/org/example/render/Mesh.java`, `render/Shader.java`.
- `src/main/resources/shaders/highlight.vert` / `highlight.frag`.
- `src/main/java/org/example/Main.java` — ordre des systèmes du `renderScheduler`.

## Spécification
1. **Extraire le raycast partagé** : créer `src/main/java/org/example/world/VoxelRaycast.java`
   (util statique, pas un système). Y déplacer la logique de raycast de `BlockInteractionSystem`,
   en retournant un résultat riche :
   ```java
   public record RaycastHit(int x, int y, int z, int faceX, int faceY, int faceZ) {}
   // faceX/Y/Z = normale entière de la face touchée (utile pour STEP-15)
   ```
   Signature suggérée : `Optional<RaycastHit> cast(float ox,oy,oz, dx,dy,dz, float reach, ChunkView)`
   où `ChunkView` est une petite interface `boolean isSolid(int wx,int wy,int wz)` pour découpler
   du stockage (réutilisable par `CollisionSystem` plus tard).
2. **Refactor** : `BlockInteractionSystem` consomme désormais `VoxelRaycast` (ne pas dupliquer).
   La face touchée est calculée en mémorisant l'axe du dernier pas dans la traversée.
3. **Composant** : `record TargetedBlock(int x,int y,int z, int faceX,int faceY,int faceZ)` posé sur
   le joueur. Écrit dans `simScheduler` par un petit `TargetingSystem` (ou par
   `BlockInteractionSystem`) à partir du raycast, retiré si rien n'est visé.
4. **Rendu** : `BlockHighlightSystem` (dans `renderScheduler`, avant l'overlay de casse) lit
   `TargetedBlock` + `RenderCamera` et dessine un **contour** :
   - soit un cube en mode lignes (`glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)` autour du draw),
   - soit un mesh d'arêtes dédié.
   Réutiliser le shader `highlight` ; couleur noire, légèrement scalé (`1.002f`) pour éviter le z-fighting.
   **Zéro allocation par frame** (matrice et mesh réutilisés, comme `BlockBreakOverlaySystem`).

## Contraintes
- `VoxelRaycast` doit être testable sans contexte GL (logique pure).
- Restaurer `glPolygonMode(GL_FILL)` après le draw pour ne pas affecter les passes suivantes.

## Tests (JUnit 5)
- `VoxelRaycastTest` : rayon droit vers un bloc solide → hit aux bonnes coordonnées + bonne normale
  de face ; rayon dans le vide → `Optional.empty()` ; respect de la portée (`reach`).
- Cas face : viser un bloc par le dessus renvoie `faceY = +1`, etc.

## Critères d'acceptation
- `/run` : un contour suit en temps réel le bloc regardé ; disparaît quand on vise le vide/au-delà de la portée.
- Le système de casse fonctionne toujours (refactor non régressif).

## Commit
`STEP-14: Add targeted-block highlight and shared voxel raycast`
