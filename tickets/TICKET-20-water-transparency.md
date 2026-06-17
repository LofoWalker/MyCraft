# STEP-20 — Eau & transparence

**Milestone :** B — Rendu fidèle · **Priorité :** 🟠 Moyenne · **Dépend de :** STEP-19

## Objectif
Rendre l'eau **semi-transparente** dans une passe translucide séparée, sans z-fighting ni faces d'eau
internes parasites, avec une surface légèrement abaissée comme dans la beta.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/ChunkMeshingSystem.java` — face culling actuel
  (`isAirOrOutOfBounds`, `appendVisibleFaces`) ; il faut séparer géométrie **opaque** et **eau**.
- `src/main/java/org/example/components/ChunkMeshComponent.java` — un mesh par chunk aujourd'hui ;
  prévoir un **second mesh** (translucide) ou un champ dédié.
- `src/main/java/org/example/systems/RenderSystem.java` — ordre de dessin ; ajouter la passe eau après
  les opaques.
- `src/main/java/org/example/systems/ChunkStreamingSystem.java` — `buildGeometry(...)` (workers) +
  upload main thread ; doit produire/uploader les deux géométries.
- `src/main/java/org/example/world/BlockType.java` / `WorldConstants.java` — `WATER` (non solide),
  `BLOCK_WATER`, `WATER_LEVEL`.
- `src/main/resources/shaders/basic.frag` — base du shader d'eau.

## Spécification
1. **Séparation des géométries** : le meshing produit deux jeux de faces — **opaque** (blocs solides)
   et **eau**. Étendre `ChunkMeshingSystem.Geometry` (ou retourner un record à deux géométries) et
   `ChunkMeshComponent` pour porter un mesh opaque + un mesh eau (l'un ou l'autre peut être vide).
2. **Culling de l'eau** : une face d'eau n'est générée que si le voisin **n'est pas de l'eau** (pas de
   faces entre deux cellules d'eau). La face supérieure de l'eau est générée si le bloc au-dessus est de
   l'air. Le culling opaque reste inchangé.
3. **Surface abaissée** : la face top de l'eau est posée à `1 - WATER_SURFACE_DROP` (constante, ex. 0.1)
   au lieu de 1.0, pour l'effet de surface beta. Les autres faces restent pleines.
4. **Passe translucide** (`RenderSystem`) : après les chunks opaques —
   `glEnable(GL_BLEND)`, `glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)`,
   `glDepthMask(false)` (depth-write off, depth-test on), dessiner les meshes d'eau **triés
   arrière→avant** par distance de chunk à la caméra, puis restaurer (`glDepthMask(true)`, `glDisable(GL_BLEND)`).
5. **Shader eau** : `resources/shaders/water.vert`/`water.frag` (ou un uniform d'alpha dans le shader
   existant). Alpha ~0.6, légère teinte bleue. Réutiliser l'atlas (tuile eau).
6. **Tri par chunk** : réutiliser la `Position` de chunk et la position caméra (`RenderCamera`/`Position`
   joueur) pour ordonner ; allouer le buffer de tri **une fois** (pas par frame).

## Contraintes
- Pas de z-fighting (depth-write off sur la passe eau, surface abaissée).
- Tri arrière→avant **sans allocation par frame** (tableau d'indices réutilisé, tri en place).
- Pas de magic number : `WATER_SURFACE_DROP`, alpha, teinte → constantes nommées.

## Tests (JUnit 5)
- `ChunkMeshingSystemTest` : une colonne eau ne génère pas de faces entre cellules d'eau adjacentes ;
  génère bien la face supérieure sous l'air ; la géométrie eau est séparée de l'opaque.
- Test util de tri arrière→avant (ordre correct pour des distances données).
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : l'eau des creux/rivières est semi-transparente, on voit le fond ; pas de z-fighting ;
  pas de faces internes entre cellules d'eau ; la surface est légèrement abaissée.

## Commit
`STEP-20: Add translucent water pass with separate mesh and depth handling`
