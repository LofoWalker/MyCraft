# STEP-33 — Gravité de blocs & random tick

**Milestone :** F — Simulation dynamique · **Priorité :** 🟢 Basse · **Dépend de :** STEP-32 (file d'updates)

## Objectif
Faire **tomber** le sable et le gravier quand ils ne sont plus soutenus, et introduire un système de
**random tick** par chunk (croissance d'herbe sur la terre nue, base pour cultures/propagation).

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/FluidSystem.java` (STEP-32) — file d'updates de blocs réutilisable
  comme modèle pour les updates de gravité.
- `src/main/java/org/example/systems/BlockInteractionSystem.java` — casse/pose déclenche un check des
  blocs voisins (le bloc au-dessus d'un bloc cassé peut tomber).
- `src/main/java/org/example/systems/ChunkStreamingSystem.java` — `ChunkDirty`/remesh ; itérer les chunks
  chargés pour le random tick.
- `src/main/java/org/example/world/BlockType.java`/`WorldConstants.java` — `BLOCK_SAND`, `BLOCK_GRAVEL`
  (à ajouter), `BLOCK_DIRT`, `BLOCK_GRASS` ; flag « affecté par la gravité ».
- `src/main/java/org/example/worldgen/stage/` — où le sable/gravier pourraient être générés (plages/rivières).
- `src/main/java/org/example/world/LightEngine.java` (STEP-21) — l'herbe ne pousse qu'avec assez de lumière.

## Spécification
1. **Blocs gravitaires** : `BLOCK_SAND`, `BLOCK_GRAVEL` (+ tuiles atlas) ; `BlockType.affectedByGravity()`.
2. **Chute de bloc** (`BlockGravitySystem`, simScheduler) : quand un bloc gravitaire perd son support
   (cellule dessous = air/fluide), il tombe. Implémentation simple recommandée : déplacer le bloc d'une
   cellule vers le bas par tick jusqu'au repos (mise à jour voxel + `ChunkDirty`), déclenché à l'édition
   et via la file d'updates (STEP-32). (Option avancée : entité de chute animée — non requise.)
3. **Déclenchement** : casser/poser près d'un bloc gravitaire enfile un check ; pas de balayage global.
4. **Random tick** (`RandomTickSystem`, simScheduler, tické) : par chunk chargé, choisir aléatoirement
   `RANDOM_TICKS_PER_CHUNK` cellules par tick et appliquer des règles :
   - **terre → herbe** si la cellule au-dessus est transparente et le niveau de lumière suffisant et qu'un
     bloc d'herbe est adjacent (propagation) ;
   - **herbe → terre** si recouverte (pas de lumière).
   Étendre plus tard (cultures) ; garder la table de règles isolée et pure.
5. **Déterminisme** : le random tick utilise un RNG seedé par chunk+tick pour rester reproductible en test.

## Contraintes
- Pas de balayage global par frame : random tick borné par chunk, gravité via file/évènements.
- Règles de tick **pures et testables** (entrée : voisinage + lumière → bloc résultant).
- Pas de magic number : `RANDOM_TICKS_PER_CHUNK`, seuil de lumière d'herbe, intervalle → constantes.

## Tests (JUnit 5)
- `BlockGravityTest` : sable suspendu (air dessous) tombe jusqu'au premier support ; sable soutenu ne
  bouge pas.
- `RandomTickRulesTest` : terre éclairée adjacente à de l'herbe se couvre d'herbe ; herbe recouverte
  redevient terre ; pas de croissance sans lumière.
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : casser le bloc sous une colonne de sable la fait s'effondrer ; une bande de terre nue à côté
  d'herbe et exposée à la lumière se recouvre progressivement d'herbe.

## Commit
`STEP-33: Add block gravity for sand/gravel and random-tick grass spread`
