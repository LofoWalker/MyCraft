# STEP-21 — Moteur de lumière (skylight + blocklight)

**Milestone :** B — Rendu fidèle · **Priorité :** 🔴 Haute (step lourd) · **Dépend de :** STEP-19

## Objectif
Calculer un champ de lumière par chunk (skylight + blocklight) propagé par **flood-fill BFS**,
recalculé à la modification de bloc, et faire que le meshing écrive un niveau de lumière par face →
assombrissement dans le shader. Introduire une source de lumière (torche).

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/components/VoxelChunkData.java` — stockage `byte[]` des block IDs, indexation
  `x + y*S + z*S*S` (ou équivalent) ; **modèle** pour un `byte[]` de lumière parallèle.
- `src/main/java/org/example/systems/ChunkMeshingSystem.java` — `MeshBuilder.addFace(...)`, format de
  vertex (8 floats après STEP-19) ; ajouter un attribut de lumière par sommet.
- `src/main/java/org/example/systems/ChunkStreamingSystem.java` — meshing sur virtual threads +
  remesh `ChunkDirty` ; la lumière se calcule **avant** le meshing, sur les workers.
- `src/main/java/org/example/worldgen/GenerationPipeline.java` — la lumière initiale peut être calculée
  juste après la génération du chunk.
- `src/main/java/org/example/world/BlockType.java` — solidité/opacité ; **ajouter** `lightEmission()`
  (torche = 14) et `opaque()` (l'eau/feuilles atténuent moins).
- `src/main/resources/shaders/basic.frag` — assombrissement par niveau de lumière.

## Spécification
1. **Stockage** : un champ lumière par chunk — `byte[]` même indexation que les blocs, 4 bits skylight
   + 4 bits blocklight par cellule (niveaux 0..15). Le porter soit dans `VoxelChunkData` (nouveau tableau)
   soit un composant `ChunkLight(byte[] light)`. Choisir cohérent avec le streaming.
2. **Skylight** : depuis le ciel, descend à 15 dans les colonnes exposées ; se propage horizontalement
   en décrémentant de 1 par bloc traversé (s'arrête sur les blocs opaques). BFS flood-fill.
3. **Blocklight** : les blocs `lightEmission() > 0` (torche=14) sèment de la lumière propagée en
   décrémentant de 1 par bloc, indépendamment du skylight.
4. **`world/LightEngine.java`** (pur, testé, sans GL) : `computeLight(VoxelChunkData) -> byte[]` ;
   BFS avec file d'indices `int[]` (pas de `HashMap` en boucle chaude). À ce step, calcul **par chunk
   isolé** acceptable (la propagation inter-chunks peut être approximée aux bords) ; documenter la limite.
5. **Recalcul à l'édition** : casser/poser un bloc (`ChunkDirty`) recalcule la lumière du chunk avant
   remesh (déjà sur le chemin `remeshDirtyChunks` / workers). Le faire **sur virtual threads** comme le
   meshing pour ne pas bloquer le thread principal.
6. **Meshing** : `addFace(...)` lit le niveau de lumière de la **cellule voisine** (celle exposée à l'air)
   et l'écrit comme attribut par sommet (1 float normalisé 0..1, ou packé). Étendre le format de vertex
   et `Mesh` (stride/attribs) en conséquence.
7. **Shader** : `basic.frag` multiplie la couleur texturée par un facteur dérivé du niveau de lumière
   (ex. `mix(MIN_LIGHT, 1.0, level)`), `MIN_LIGHT` constante (grottes sombres mais pas noir absolu).
8. **Torche** : nouveau `BlockType TORCH` (`lightEmission=14`, non solide ou petit), `BLOCK_TORCH` dans
   `WorldConstants`, tuile d'atlas, plaçable via la hotbar (réutilise STEP-15/16).

## Contraintes
- BFS **sans allocation par cellule** : files primitives `int[]` réutilisées, pas de `HashMap`/`List`
  dans la boucle de propagation.
- Lumière calculée sur **virtual threads** (gen/meshing), jamais sur le thread de rendu.
- Pas de magic number : niveaux max, `MIN_LIGHT`, émission torche → constantes (`WorldConstants`/`LightEngine`).

## Tests (JUnit 5)
- `LightEngineTest` : colonne d'air exposée → skylight 15 en surface, décrément correct sous un surplomb ;
  une torche entourée d'air → halo décroissant 14,13,…,0 ; un mur opaque bloque la propagation.
- Déterminisme (même chunk → même champ lumière).
- `ChunkMeshingSystemTest` : la face exposée porte le niveau de lumière de la cellule voisine.
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : grottes/intérieurs sombres, surfaces extérieures pleinement éclairées ; poser une torche
  éclaire un rayon décroissant ; faces orientées différemment correctement nuancées ; casser/poser
  met la lumière à jour sans freeze.

## Commit
`STEP-21: Add skylight and blocklight flood-fill with per-face vertex lighting`
