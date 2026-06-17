# STEP-16 — Inventaire & hotbar (données)

**Milestone :** A — Interaction · **Priorité :** 🔴 Haute · **Dépend de :** STEP-15

## Objectif
Modéliser l'inventaire et la hotbar côté données (sans UI), sélectionner un slot, et faire que le
bloc posé (STEP-15) soit l'item du slot actif.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/components/` — convention des records data-only.
- `src/main/java/org/example/world/BlockType.java`, `WorldConstants.java` — IDs de blocs.
- `src/main/java/org/example/systems/InputSystem.java` — pour ajouter touches 1-9 + molette.
- `src/main/java/org/example/systems/BlockInteractionSystem.java` — méthode `selectedBlock(...)` du STEP-15.
- `src/main/java/org/example/Window.java` — vérifier la dispo d'un callback molette (`glfwSetScrollCallback`).

## Spécification
1. **Records** (data-only, dans `components/`) :
   - `ItemStack(int itemId, int count)` — `itemId` réutilise les IDs de blocs pour l'instant
     (un item = un bloc plaçable). Prévoir `count <= MAX_STACK` (constante, ex. 64).
   - `Inventory(ItemStack[] slots)` — taille fixe (ex. 36 = 9 hotbar + 27 sac), `null`/`ItemStack`
     vide géré par une sentinelle `ItemStack.EMPTY` (count 0) **plutôt que** `null`.
   - `Hotbar(int selectedSlot)` — index 0..8.
   > ⚠️ Les records sont immuables : les mutations (ajout/retrait) **retournent un nouveau record**
   > ou opèrent sur le tableau interne. Choisir une approche cohérente avec les règles ECS
   > (préférer des **méthodes utilitaires pures** dans une classe `world/Inventories.java` plutôt
   > que de la logique dans le record).
2. **Logique d'inventaire** (classe util `world/Inventories.java`, pure, testée) :
   - `add(Inventory, ItemStack) -> Inventory` : empile sur un stack existant du même item puis
     remplit un slot vide ; renvoie l'éventuel reste.
   - `removeOne(Inventory, slot) -> Inventory`, `get(Inventory, slot)`.
3. **Sélection de slot** : étendre `PlayerInput` avec `int selectedSlotDelta` (molette) et/ou
   `int hotbarSelect` (touches 1-9, -1 si aucune). `InputSystem` lit `glfwSetScrollCallback`
   (accumulé comme les deltas souris) et les touches `GLFW_KEY_1..9`.
   Un `HotbarSelectionSystem` (sim) applique l'input → met à jour `Hotbar.selectedSlot` (wrap 0..8).
4. **Brancher la pose** : `selectedBlock(world, player)` retourne l'`itemId` du slot actif de la
   hotbar ; la pose décrémente le stack (au STEP-18 les drops rempliront l'inventaire).
   En attendant les drops, donner au joueur un inventaire de départ dans `Main` (quelques stacks).

## Contraintes
- Pas de `null` dans l'API : utiliser `ItemStack.EMPTY`.
- `Inventories` sans allocation superflue dans les chemins appelés en boucle (la pose est edge-trigger,
  donc pas hot — clarté prioritaire ici).

## Tests (JUnit 5)
- `InventoriesTest` : empilement jusqu'à `MAX_STACK` + débordement sur slot suivant ; ajout dans
  inventaire plein renvoie le reste ; `removeOne` décrémente/vide correctement.
- `HotbarSelectionSystemTest` : molette et touches 1-9 changent le slot avec wrap.

## Critères d'acceptation
- `/run` : molette/touches 1-9 changent le slot actif ; poser consomme l'item du slot ; quand le
  stack est vide, la pose de cet item devient impossible.
- Aucune UI requise à ce step (validation par logs/tests).

## Commit
`STEP-16: Add inventory, hotbar and item stacks with slot selection`
