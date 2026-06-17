# STEP-27 — Craft (2×2 + table 3×3)

**Milestone :** D — Items, craft & blocs · **Priorité :** 🔴 Haute (step lourd) · **Dépend de :** STEP-26

## Objectif
Ajouter le craft : grille 2×2 dans l'inventaire et bloc **établi** (3×3), avec un livre de recettes
(shaped/shapeless) et une UI d'inventaire modale (curseur libre).

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/world/Inventories.java`, `components/Inventory.java`, `ItemStack.java`,
  `Hotbar.java` (STEP-16) — base de stockage et de manipulation des stacks.
- `src/main/java/org/example/world/ItemRegistry.java` (STEP-26) — items résultants/ingrédients.
- `src/main/java/org/example/systems/HudSystem.java` (STEP-17) + `render/BitmapFont.java` — base du rendu
  2D pour l'UI d'inventaire (réutiliser le pipeline ortho/quads/texte).
- `src/main/java/org/example/systems/InputSystem.java`, `components/PlayerInput.java`,
  `src/main/java/org/example/Window.java` — touche d'ouverture d'inventaire (E), position curseur
  (`glfwSetCursorPosCallback` / mode curseur libre vs capturé), clic.
- `src/main/java/org/example/world/BlockType.java`/`WorldConstants.java` — ajouter `CRAFTING_TABLE`.

## Spécification
1. **Modèle de recettes** (`world/RecipeBook.java`, pur, testé) :
   - `record ShapedRecipe(int width, int height, int[] pattern, ItemStack result)` et
     `record ShapelessRecipe(int[] ingredients, ItemStack result)`.
   - `match(int[] grid, int gridW, int gridH) -> Optional<ItemStack>` : gère le **décalage** du motif
     dans la grille (shaped) et l'ordre indifférent (shapeless). Pas de `null`.
2. **État de craft** : `record CraftingGrid(ItemStack[] slots, int size)` (size 2 ou 3) + `craftResult`
   dérivé. Composant attaché à une entité « UI ouverte » ou au joueur pendant l'ouverture.
3. **UI d'inventaire** (`InventoryScreenSystem`, render + input) : ouverte avec **E**, libère le curseur,
   met le jeu en « pause d'interaction » (pas de break/place tant qu'ouvert). Affiche sac + hotbar +
   grille de craft + slot résultat. Curseur libre : prendre/poser des stacks au clic (held stack),
   répartition shift-clic optionnelle.
4. **Établi** : `CraftingTable` bloc (`BLOCK_CRAFTING_TABLE`, tuile atlas) ; clic droit dessus ouvre la
   grille **3×3** ; sans établi, l'inventaire offre la grille **2×2**.
5. **Validation craft** : à chaque changement de grille, `RecipeBook.match(...)` met à jour le slot
   résultat ; prendre le résultat consomme 1 de chaque ingrédient. Recettes de base : planches (bois→4
   planches, shapeless), établi (4 planches), bâtons, pioche/hache/pelle/épée (bois/pierre), torches.
6. **Câblage** : enregistrer `InventoryScreenSystem` (dernier du renderScheduler, après le HUD) ;
   gérer le **toggle capture curseur** dans `Window`/`InputSystem`.

## Contraintes
- `RecipeBook` et `match(...)` **purs**, sans GL ni World — entièrement testables.
- Pas de `null` (utiliser `ItemStack.EMPTY`). Pas de magic number (tailles de grille, layouts → constantes).
- L'UI **lit** l'ECS et écrit l'inventaire via `Inventories` ; pas de logique de craft dans le rendu.

## Tests (JUnit 5)
- `RecipeBookTest` : recette shaped reconnue quel que soit le décalage dans la grille ; refus si motif
  incorrect ; shapeless indépendante de l'ordre ; consommation correcte des ingrédients.
- `CraftingTest` : bois→planches→établi→pioche (chaîne) ; prendre le résultat décrémente les ingrédients.
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : E ouvre l'inventaire (curseur libre, jeu en pause d'interaction) ; craft 2×2 fonctionne ;
  poser/utiliser un établi ouvre le 3×3 ; les recettes shaped et shapeless produisent les bons items.

## Commit
`STEP-27: Add crafting grids, recipe book and inventory screen`
