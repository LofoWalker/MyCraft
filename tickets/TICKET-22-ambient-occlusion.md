# STEP-22 — Ambient occlusion (smooth lighting)

**Milestone :** B — Rendu fidèle · **Priorité :** 🟢 Basse · **Dépend de :** STEP-21

## Objectif
Calculer une **ambient occlusion par sommet** au meshing (coins de voxels), combinée à la lumière du
STEP-21, pour l'aspect « smooth lighting » de la beta (coins et recoins légèrement assombris).

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/ChunkMeshingSystem.java` — `MeshBuilder.addFace(...)`, `FACE_OFFSETS`,
  ordre des 4 sommets par face ; c'est là qu'on calcule l'AO par sommet.
- `src/main/java/org/example/world/LightEngine.java` (STEP-21) — combiner AO et niveau de lumière.
- `src/main/resources/shaders/basic.vert` / `basic.frag` — l'attribut lumière/AO est interpolé puis
  appliqué.
- Référence algorithme : « 0fps — Ambient Occlusion for Minecraft-like worlds » (4 niveaux d'AO selon
  les 3 voxels voisins de chaque coin : 2 côtés + 1 coin).

## Spécification
1. **AO par sommet** : pour chaque sommet d'une face, regarder les **3 voisins** du coin (les deux blocs
   adjacents `side1`/`side2` et le bloc de `corner`). Niveau AO 0..3 :
   `if (side1 && side2) ao = 0; else ao = 3 - (side1 + side2 + corner)`. Convertir en facteur (ex. 0.5..1.0).
2. **Combinaison** : facteur final par sommet = `lightFactor(STEP-21) * aoFactor`. Écrit comme attribut
   par sommet (réutiliser/élargir l'attribut lumière existant ; éviter d'ajouter un attribut séparé si un
   seul float suffit).
3. **Anti-anisotropie (flip de quad)** : si les AO des coins opposés sont déséquilibrés, **inverser la
   diagonale** de triangulation du quad (ordre des indices) pour éviter l'artefact de dégradé en biais.
   Adapter l'émission des indices dans `addFace(...)` selon le cas.
4. **Coût** : l'AO se calcule pendant le meshing déjà sur virtual threads — pas de surcoût sur le thread
   de rendu. Lecture des voisins via `VoxelChunkData.get(...)` (bornes gérées comme `isAirOrOutOfBounds`).

## Contraintes
- Zéro allocation supplémentaire par face (réutiliser les buffers du `MeshBuilder`).
- Pas de magic number : table des facteurs AO (4 valeurs) → constante nommée.
- Ne pas régresser le format de vertex au-delà du nécessaire (idéalement aucun nouvel attribut).

## Tests (JUnit 5)
- `ChunkMeshingSystemTest` (ou `AmbientOcclusionTest`) : un coin entouré de blocs pleins reçoit l'AO le
  plus sombre (0) ; un coin dégagé reçoit l'AO clair (3) ; vérifier le flip de diagonale sur un cas
  déséquilibré (ordre des indices attendu).
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : coins et recoins (sous les surplombs, contre les murs, dans les angles) légèrement
  assombris ; transition douce ; pas d'artefact de diagonale visible ; aspect « beta smooth lighting ».

## Commit
`STEP-22: Add per-vertex ambient occlusion with anisotropy quad flip`
