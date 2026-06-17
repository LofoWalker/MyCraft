# STEP-37 — Menus & modes de jeu

**Milestone :** G — Menus · **Priorité :** 🟠 Moyenne · **Dépend de :** STEP-35 (save/load), STEP-17 (HUD/2D)

## Objectif
Ajouter un menu principal (nouveau / charger un monde), un menu pause, et la sélection
**Survie / Créatif** (vol + ressources infinies + pas de dégâts en créatif).

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/GameLoop.java` — boucle principale (`run(...)`) ; il faut une notion d'**état
  d'application** (MENU / IN_GAME / PAUSED) qui conditionne quels schedulers tournent.
- `src/main/java/org/example/Main.java` — assemblage du monde, schedulers, `try (...)` des sous-systèmes.
- `src/main/java/org/example/systems/HudSystem.java` (STEP-17) + `render/BitmapFont.java` — rendu 2D
  (texte/quads) pour les écrans de menu.
- `src/main/java/org/example/systems/InventoryScreenSystem.java` (STEP-27) — modèle d'écran modal avec
  curseur libre + capture d'input (réutiliser le pattern pour les menus).
- `src/main/java/org/example/io/WorldStorage.java` (STEP-35) — lister/charger/créer des mondes.
- `src/main/java/org/example/systems/FlightControlSystem.java`, `components/Flying.java`, `Gravity.java`,
  `PhysicsSystem.java` — le créatif active le vol (déjà présent) et désactive gravité/dégâts.
- `src/main/java/org/example/systems/HealthSystem.java` (STEP-24) — désactiver les dégâts en créatif.

## Spécification
1. **État d'application** : `enum AppState { MAIN_MENU, IN_GAME, PAUSED }` piloté dans `GameLoop`/`Main`.
   Selon l'état : MENU/PAUSED ne lancent pas le simScheduler de gameplay, mais une passe d'UI ;
   IN_GAME lance simulation + rendu normalement.
2. **Menu principal** (`MainMenuSystem`, rendu 2D) : boutons « Nouveau monde », « Charger » (liste les
   dossiers de `saves/` via `WorldStorage`), « Quitter ». Curseur libre, navigation clavier/souris.
   « Nouveau » demande/par défaut un nom + mode de jeu (Survie/Créatif) puis crée le monde et passe IN_GAME.
3. **Menu pause** (`PauseMenuSystem`) : ouvert avec **Échap** en jeu — fige la simulation, affiche
   « Reprendre », « Sauvegarder et quitter » (appelle STEP-35), libère le curseur ; reprise recapture le
   curseur et réactive la simulation.
4. **Modes de jeu** : `record GameMode(Mode mode)` (`SURVIVAL`/`CREATIVE`) sur le joueur ou le world.
   - CREATIVE : `Flying` activé par défaut (vol libre), **pas de gravité forcée**, **pas de dégâts**
     (`HealthSystem` ignore), casse instantanée, ressources infinies (la pose/usage ne décrémente pas les
     stacks), inventaire créatif (accès à tous les blocs).
   - SURVIVAL : comportement actuel (gravité, dégâts, ressources limitées).
   Brancher ces conditions dans les systèmes concernés (mining/placement/health/physique) via `GameMode`.
5. **Persistance du mode** : le mode est sauvé/chargé dans `level.dat` (STEP-35).
6. **Câblage** : `GameLoop` aiguille les schedulers selon `AppState` ; gestion centralisée de la capture
   curseur (menus = libre, jeu = capturé).

## Contraintes
- Le basculement d'état ne doit pas fuiter de ressources (mondes chargés/fermés proprement).
- Les conditions de mode (créatif/survie) lues via `GameMode`, **sans dupliquer** la logique de gameplay
  (un seul point de décision par système).
- Pas de magic number : tailles/positions de boutons, libellés → constantes/ressources. Pas de `null`.

## Tests (JUnit 5)
- `GameModeTest` : en créatif, la pose ne décrémente pas le stack et la casse est instantanée ; en survie,
  la pose décrémente et la casse suit la hardness (fonctions de décision pures).
- `MainMenuTest` : `WorldStorage` liste correctement les mondes existants ; « Nouveau » crée un `level.dat`.
- Test de transition d'état (`AppState`) : pause fige la simulation, reprise la réactive (logique pure).
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : démarrage sur le menu principal ; créer un nouveau monde (choix Survie/Créatif) ou en charger
  un existant ; Échap ouvre la pause (sauver & quitter fonctionne) ; en créatif on vole, on ne subit pas
  de dégâts et les ressources sont illimitées ; le mode est restauré au rechargement.

## Commit
`STEP-37: Add main/pause menus, world selection and survival/creative modes`
