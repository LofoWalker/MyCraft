# STEP-26 — Registre d'items & outils

**Milestone :** D — Items, craft & blocs · **Priorité :** 🔴 Haute · **Dépend de :** STEP-16, STEP-18

## Objectif
Introduire un vrai **registre d'items** (au-delà du « item = bloc ») avec types, taille de stack et
durabilité, et des familles d'outils (pioche/hache/pelle/épée) qui modulent la **vitesse de minage** et
les **drops** selon le matériau.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/components/ItemStack.java` (STEP-16) — `itemId` réutilise les IDs de blocs.
- `src/main/java/org/example/world/Inventories.java` — `MAX_STACK` ; à généraliser par item.
- `src/main/java/org/example/world/BlockType.java` — `hardness()` (hits mains nues) ; modèle de table.
- `src/main/java/org/example/systems/BlockInteractionSystem.java` — `accumulatedDamage(...)`,
  `BARE_HAND_DAMAGE`, `damageBlock(...)` : la vitesse de casse à moduler par l'outil tenu.
- `src/main/java/org/example/systems/ItemPickupSystem.java` / drops (STEP-18) — `removeBlock(...)` spawn
  le drop ; à brancher sur la logique de drop dépendant de l'outil.

## Spécification
1. **`world/ItemRegistry.java`** : table immuable indexée par `itemId`. Au-delà des IDs de blocs
   (réservés), ajouter des IDs d'**outils**. Pour chaque item : `maxStack`, `isBlock`, `isTool`,
   `ToolKind` (PICKAXE/AXE/SHOVEL/SWORD/NONE), `ToolMaterial` (WOOD/STONE/IRON/DIAMOND…), `durability`.
   Lookup par index (`itemId`) — pas de `HashMap` en hot path.
2. **`ToolKind` / `ToolMaterial`** (enums) : matériau → niveau de minage + multiplicateur de vitesse.
   `BlockType` indique la **famille d'outil efficace** (pierre→pioche) et un **niveau de minage requis**
   (diamant ne droppe qu'avec pioche fer+).
3. **Vitesse de minage** : généraliser la logique de hits — `damagePerHit(blockType, heldItem)` au lieu de
   `BARE_HAND_DAMAGE` constant. Bon outil → casse plus vite ; mauvais/mains nues → lent.
4. **Drops conditionnels** : à la casse (STEP-18), décider le drop selon `(blockType, heldTool)` :
   pierre mains nues → toujours (cobble), minerai diamant → drop **seulement** si pioche fer+ ; sinon rien.
   Centraliser dans `world/BlockDrops.java` (pur, testé).
5. **Durabilité** : `ItemStack` outil porte une durabilité courante (champ supplémentaire ou composant
   `Durability` lié au slot). Chaque casse décrémente ; à 0, l'outil disparaît. Garder l'`ItemStack`
   data-only (mutation via util `Inventories`).
6. **Stack par item** : `Inventories.add(...)` utilise `ItemRegistry.maxStack(itemId)` (outils = 1).

## Contraintes
- Lookups par index (arrays), aucune `HashMap` dans la résolution de minage/drop (chemins appelés à la casse).
- Pas de magic number : niveaux de minage, multiplicateurs, durabilités → `ItemRegistry`/constantes.
- Compat ascendante : « item = bloc » du STEP-16 reste valide (les blocs ont leur entrée registre).

## Tests (JUnit 5)
- `ItemRegistryTest` : propriétés correctes par item (stack, outil, matériau, durabilité).
- `BlockDropsTest` : diamant droppe avec pioche fer+, rien avec pioche pierre/mains ; pierre droppe avec pioche.
- `MiningSpeedTest` : `damagePerHit` plus élevé avec le bon outil/matériau qu'à mains nues.
- `/test` complet sans régression (notamment `BlockInteractionSystemTest`).

## Critères d'acceptation
- `/run` : une pioche casse la pierre nettement plus vite qu'à la main ; casser un minerai de diamant
  sans pioche fer ne donne aucun drop ; les outils s'usent et finissent par casser.

## Commit
`STEP-26: Add item registry with tools, mining speed and conditional drops`
