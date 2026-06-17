# STEP-29 — Cadre entités mobiles + rendu

**Milestone :** E — Monde vivant · **Priorité :** 🔴 Haute · **Dépend de :** STEP-10 (physique/collision), STEP-19 (textures)

## Objectif
Poser le socle des entités mobiles (mobs) : composants de mob et d'IA, et un système de rendu de modèles
simples (boîtes texturées) dans le monde 3D, validé par une entité de test qui se déplace.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/PhysicsSystem.java`, `CollisionSystem.java`, `MovementSystem.java` —
  la physique opère sur `Position + Velocity + Gravity + ColliderAABB` ; les mobs réutilisent ce socle
  (cf. drops STEP-18 qui réutilisent déjà la physique).
- `src/main/java/org/example/systems/RenderSystem.java` — modèle de passe (uniforms `uView/uProjection/uModel`,
  bind shader, frustum) ; le rendu de mobs est une passe analogue.
- `src/main/java/org/example/render/Mesh.java` — création de mesh à partir de vertices/indices ;
  réutiliser pour un cube unité texturé.
- `src/main/java/org/example/components/Position.java`, `Velocity.java`, `Rotation.java`, `ColliderAABB.java`.
- `src/main/java/org/example/render/Frustum.java` — culling (ne pas rendre les mobs hors champ).

## Spécification
1. **Composants mob** (records data-only) : `MobType` (enum-backed id : COW/PIG/SHEEP/CHICKEN/ZOMBIE…),
   `Health` (réutilise STEP-24), `AiState` (enum : IDLE/WANDER/FLEE/CHASE + timer), `PathTarget(float x,y,z)`.
   Un mob = `Position + Velocity + Gravity + ColliderAABB + Rotation + MobType + Health + AiState`.
2. **Spawn util** : `world/Mobs.java` — `spawn(World, MobType, x,y,z)` qui assemble les composants
   (taille de collider et vie par `MobType`). Pas de logique de spawn-rules ici (STEP-30/31).
3. **`EntityRenderSystem`** (renderScheduler, après les chunks, avant le HUD) : pour chaque entité
   `MobType + Position`, dessine un **modèle boîte** (corps + tête, échelle par `MobType`) avec l'atlas
   (UV de mob ; un sous-atlas ou une texture dédiée). `uModel` = translation+rotation (yaw) ; réutiliser
   les matrices en place (zéro alloc/frame), frustum-cull par AABB.
4. **Modèle** : un `Mesh` de cube unité texturé partagé (créé une fois), instancié par mob via `uModel`
   et une mise à l'échelle. Modèle multi-parties optionnel (boîtes empilées) — corps + tête suffit.
5. **Entité de test** : dans `Main` (ou un système de debug), spawner un mob qui se déplace en ligne/
   cercle (sans IA complète) pour valider physique + rendu.

## Contraintes
- Mobs soumis à la **même physique** que le joueur (gravité + collision voxel), sans `PlayerInput`.
- Rendu **sans allocation par frame** (matrices/buffers réutilisés), frustum culling appliqué.
- Pas d'héritage : un mob est un assemblage de composants (records), `MobType` ne porte pas de logique.

## Tests (JUnit 5)
- `MobsTest` : `spawn(...)` crée une entité avec les composants attendus et les bons paramètres
  (collider/vie) selon `MobType`.
- Test (sans GL) du calcul de `uModel` (translation + yaw + échelle) ou de la sélection d'UV par `MobType`.
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : une entité de test (boîte texturée) apparaît, tombe au sol, marche sur le terrain, est
  correctement orientée et disparaît du rendu hors champ.

## Commit
`STEP-29: Add mob component framework and box-model entity rendering`
