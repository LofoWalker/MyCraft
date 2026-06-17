# STEP-28 — Fourneau & coffre

**Milestone :** D — Items, craft & blocs · **Priorité :** 🟠 Moyenne · **Dépend de :** STEP-27

## Objectif
Ajouter deux blocs fonctionnels avec inventaire propre : le **fourneau** (cuisson entrée + combustible →
sortie) et le **coffre** (stockage persistant lié au bloc).

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/world/RecipeBook.java` (STEP-27) — modèle de recettes ; ajouter des recettes
  de **fonte** (smelting) analogues.
- `src/main/java/org/example/world/Inventories.java`, `components/ItemStack.java`, `Inventory.java` —
  stockage des slots des blocs.
- `src/main/java/org/example/systems/InventoryScreenSystem.java` (STEP-27) — UI modale réutilisable pour
  fourneau/coffre.
- `src/main/java/org/example/systems/BlockInteractionSystem.java` — clic droit sur un bloc (ouvrir l'UI
  du bloc) ; distinguer « poser » vs « interagir » selon le bloc visé.
- `src/main/java/org/example/world/BlockType.java`/`WorldConstants.java` — ajouter `FURNACE`, `CHEST`.

## Spécification
1. **Blocs** : `BLOCK_FURNACE`, `BLOCK_CHEST` (+ tuiles atlas, faces orientées pour le fourneau).
   Clic droit dessus **ouvre l'UI du bloc** au lieu de poser.
2. **Liaison bloc → inventaire** : un bloc fonctionnel a un inventaire propre indexé par sa **position
   monde**. Stocker dans un composant porté par une entité « block-entity » (ex.
   `record BlockEntity(int wx, int wy, int wz)` + `Inventory`), retrouvée par position. Lookup hors hot
   path (interaction edge-triggered) — une `Map<Long,int>` position→entité est acceptable ici.
3. **Coffre** : `Inventory` (ex. 27 slots) ; l'UI (réutiliser `InventoryScreenSystem`) permet de
   transférer entre coffre et joueur. À la casse du bloc, **drop du contenu** (STEP-18).
4. **Fourneau** : `record Furnace(int cookTicks, int fuelTicks, int fuelTicksMax)` + 3 slots
   (entrée/combustible/sortie). `FurnaceSystem` (simScheduler) : si entrée fusible + combustible dispo +
   sortie compatible, consomme du combustible, avance la cuisson ; à `COOK_TIME` atteint, produit la
   sortie et consomme 1 entrée. Tables `smeltingResult(itemId)` et `fuelBurnTicks(itemId)` (pures).
5. **UI** : layout fourneau (entrée au-dessus du combustible, flèche de progression, sortie) et coffre
   (grille). Barre de progression cuisson + jauge de combustible dessinées via le pipeline 2D.
6. **Câblage** : `FurnaceSystem` dans le simScheduler ; ouverture/fermeture d'UI gérée comme STEP-27.

## Contraintes
- `FurnaceSystem` opère sur des **ticks de simulation** (dt fixe), pas sur le rendu.
- Tables de fonte/combustible **pures et testées**. Pas de magic number (`COOK_TIME`, burn ticks → constantes).
- Pas de `null` (slots = `ItemStack.EMPTY`). Le drop à la casse ne perd aucun item.

## Tests (JUnit 5)
- `FurnaceSystemTest` : minerai + combustible → lingot après `COOK_TIME` ; sans combustible, pas de
  cuisson ; combustible consommé par paliers ; sortie pleine bloque la cuisson.
- `ChestTest` : transfert d'items joueur↔coffre ; casser le coffre droppe tout le contenu.
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : poser un fourneau, y fondre un minerai en lingot avec du combustible (progression visible) ;
  poser un coffre, y stocker/récupérer des items ; casser un coffre plein restitue le contenu au sol.

## Commit
`STEP-28: Add functional furnace smelting and chest storage block entities`
