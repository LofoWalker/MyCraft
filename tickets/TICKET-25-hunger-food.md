# STEP-25 — Faim & nourriture (beta 1.8)

**Milestone :** C — Survie · **Priorité :** 🟠 Moyenne · **Dépend de :** STEP-24

## Objectif
Ajouter la jauge de faim style beta 1.8 : elle baisse à l'effort, régule la régénération de vie, et
affame le joueur à 0. Items consommables qui restaurent la faim, et barre de nourriture au HUD.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/HealthSystem.java` (STEP-24) — régénération de vie à coupler à la faim.
- `src/main/java/org/example/components/` — `Health` (STEP-24), conventions records.
- `src/main/java/org/example/components/Velocity.java`, `Grounded.java` — détecter l'effort (sprint/saut).
- `src/main/java/org/example/world/Inventories.java` (STEP-16), `components/ItemStack.java`,
  `Hotbar.java` — consommer un item depuis le slot actif.
- `src/main/java/org/example/systems/InputSystem.java`, `components/PlayerInput.java` — input « manger »
  (ex. clic droit avec un item de nourriture sélectionné).
- `src/main/java/org/example/systems/HudSystem.java` (STEP-17) — barre de nourriture.

## Spécification
1. **Composant** : `record Hunger(int food, float saturation)` — `food` 0..`MAX_FOOD` (ex. 20),
   `saturation` 0..food (réservoir caché beta 1.8).
2. **Épuisement** : `HungerSystem` (simScheduler) accumule un « exhaustion » selon l'activité (déplacement,
   saut, dégâts subis) ; à chaque palier `EXHAUSTION_THRESHOLD`, retirer 1 de `saturation`, puis de `food`
   quand la saturation est vide.
3. **Couplage vie** (dans `HealthSystem`/`HungerSystem`) :
   - `food >= REGEN_FOOD_THRESHOLD` (ex. 18) → régénère lentement la vie (consomme de la saturation/faim).
   - `food == 0` → dégâts de famine périodiques (mais pas en dessous d'un plancher de vie selon difficulté ;
     pour ce projet, peut descendre à 0).
4. **Items consommables** : étendre le registre d'items (ou une table simple `world/Foods.java`) avec des
   valeurs `(foodRestore, saturationRestore)` par itemId (pomme, viande…). Manger l'item sélectionné
   (input dédié) → restaure faim/saturation et **décrémente le stack** (`Inventories.removeOne`).
5. **HUD** : barre de nourriture (icônes) à côté/au-dessus des cœurs, reflétant `food`.
6. **Câblage** : ajouter `Hunger` au joueur dans `Main` ; enregistrer `HungerSystem` dans le simScheduler.

## Contraintes
- Logique d'épuisement/régen **pure et testable** (séparée de l'accès World).
- Pas de magic number : `MAX_FOOD`, `EXHAUSTION_THRESHOLD`, `REGEN_FOOD_THRESHOLD`, coûts d'activité,
  intervalle de famine, tables de nourriture → constantes nommées.
- Pas de `null` (items via `ItemStack.EMPTY`).

## Tests (JUnit 5)
- `HungerSystemTest` : l'effort accumule l'exhaustion et fait baisser saturation puis food ; manger
  restaure food/saturation et décrémente le stack ; `food==0` inflige des dégâts de famine.
- Test couplage : `food >= seuil` régénère la vie en consommant de la faim.
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : la barre de faim baisse en se déplaçant/sautant ; manger la remonte ; faim pleine régénère
  lentement la vie ; faim à 0 fait perdre de la vie ; le HUD affiche cœurs + nourriture.

## Commit
`STEP-25: Add hunger and food restoration with health regen coupling`
