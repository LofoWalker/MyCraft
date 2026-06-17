# STEP-18 — Drops d'items & ramassage

**Milestone :** A — Interaction · **Priorité :** 🟠 Moyenne · **Dépend de :** STEP-16 (STEP-17 pour le retour visuel)

## Objectif
Casser un bloc fait apparaître une entité-item au sol, soumise à la physique, que le joueur ramasse
en s'approchant (l'item entre dans l'`Inventory`).

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/BlockInteractionSystem.java` — `removeBlock(...)` : point où
  spawner le drop.
- `src/main/java/org/example/systems/PhysicsSystem.java`, `CollisionSystem.java`, `MovementSystem.java`
  — la physique opère sur `Position + Velocity + (Gravity) + ColliderAABB` ; un item peut réutiliser
  ces composants.
- `src/main/java/org/example/components/` — `Position`, `Velocity`, `Gravity`, `ColliderAABB`, `ItemStack`.
- `src/main/java/org/example/world/Inventories.java` (STEP-16) — `add(...)`.
- `src/main/java/org/example/systems/RenderSystem.java` ou `BlockBreakOverlaySystem.java` — pour rendre
  les items (petit cube). Voir aussi STEP-29 (rendu d'entités) si tu préfères mutualiser.

## Spécification
1. **Marqueur** : `record ItemEntity()` (tag) + `ItemStack` + `Position` + `Velocity` + `Gravity` +
   `ColliderAABB` (petit, ex. 0.25³). Optionnel `record PickupDelay(float seconds)` pour éviter de
   ramasser instantanément l'item qu'on vient de lâcher.
2. **Spawn au cassage** : quand `BlockInteractionSystem` retire un bloc, créer une entité item au
   centre du bloc avec une petite impulsion verticale/aléatoire. L'`itemId` = bloc cassé (drops plus
   fins — outils, diamant pioche-only — viendront au STEP-26).
3. **Physique** : les items tombent et reposent au sol (réutiliser `PhysicsSystem`/`CollisionSystem` ;
   vérifier qu'ils n'ont pas besoin de `PlayerInput`). Friction au sol optionnelle.
4. **Ramassage** : `ItemPickupSystem` (sim) : pour chaque `ItemEntity` à distance < `PICKUP_RADIUS`
   du joueur (et `PickupDelay` écoulé), `Inventories.add(...)` dans l'`Inventory` du joueur ;
   si tout est absorbé, `world.destroy(item)`. Sinon, mettre à jour le `ItemStack` restant.
   Aimantation optionnelle : attirer l'item vers le joueur dans un rayon plus large.
5. **Rendu** : `ItemRenderSystem` (render) dessine un petit cube (couleur/texture du bloc) à la
   position de l'item ; rotation lente facultative. Zéro alloc par frame.

## Contraintes
- `PICKUP_RADIUS`, taille du collider, impulsion → constantes nommées (`WorldConstants`).
- Le ramassage respecte la capacité d'inventaire (un inventaire plein laisse l'item au sol).

## Tests (JUnit 5)
- `ItemPickupSystemTest` : item dans le rayon + délai écoulé → entre en inventaire et l'entité est
  détruite ; hors rayon → ignoré ; inventaire plein → item conservé au sol.

## Critères d'acceptation
- `/run` : casser un bloc fait tomber un item visible ; marcher dessus le ramasse ; la hotbar (HUD)
  reflète le gain ; un inventaire plein ne ramasse pas.

## Commit
`STEP-18: Add item drops with physics and player pickup`
