# STEP-31 — Mobs hostiles + combat + IA

**Milestone :** E — Monde vivant · **Priorité :** 🔴 Haute (step lourd) · **Dépend de :** STEP-30, STEP-24

## Objectif
Ajouter des mobs hostiles (zombie/squelette/araignée/creeper) qui apparaissent à faible lumière,
traquent le joueur par **pathfinding A\***, attaquent au contact, et brûlent au jour pour les morts-vivants ;
plus le combat joueur (épée, knockback).

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/MobAiSystem.java`, `components/AiState.java`, `PathTarget.java`
  (STEP-30) — étendre l'IA avec l'état CHASE et le pathfinding.
- `src/main/java/org/example/world/LightEngine.java` (STEP-21) — niveau de lumière d'une cellule (règle
  de spawn faible lumière + combustion au jour).
- `src/main/java/org/example/components/Health.java`, `DamageImmunity.java` (STEP-24) — dégâts joueur↔mob.
- `src/main/java/org/example/systems/BlockInteractionSystem.java` — clic gauche edge-trigger ; modèle pour
  l'attaque joueur (raycast/portée d'attaque).
- `src/main/java/org/example/world/ItemRegistry.java` (STEP-26) — épée → dégâts d'attaque.
- `src/main/java/org/example/components/Velocity.java` — knockback (impulsion).
- `src/main/java/org/example/systems/CollisionSystem.isSolid(...)` — voisinage pour le graphe A*.

## Spécification
1. **Pathfinding A\*** (`world/PathFinder.java`, pur, testé) : recherche sur la grille de blocs
   (voisins marchables : sol solide dessous, 2 blocs d'air, saut d'1 bloc), heuristique Chebyshev,
   **budget de nœuds borné** par appel. File de priorité sur `int[]`/structures primitives — pas de
   `HashMap` en boucle chaude (clés encodées en `long`). Renvoie le prochain waypoint, pas tout le chemin.
2. **IA hostile** : nouvel état CHASE dans `MobAiSystem` — si le joueur est à portée de détection et en
   ligne de mire, recalculer périodiquement un `PathTarget` via A* et avancer ; attaquer au contact
   (`MOB_ATTACK_DAMAGE`, cadence + i-frames via `DamageImmunity`). Creeper : approche puis explosion
   (différée) — au minimum dégâts de zone (la destruction de blocs peut être un TODO documenté).
3. **Spawn hostile** (`HostileSpawnSystem`) : à faible **lumière** (`LightEngine` < seuil) dans les chunks
   chargés, hors d'un rayon minimal du joueur, cap de population. Indépendant du jour mais favorisé la nuit
   (la baisse de skylight STEP-23 crée des zones sombres).
4. **Combustion au jour** : les morts-vivants (zombie/squelette) exposés au ciel avec skylight élevé de
   jour subissent des dégâts de feu réguliers.
5. **Combat joueur** : clic gauche **sur un mob** dans la portée d'attaque inflige
   `weaponDamage(heldItem)` (épée > main) + **knockback** (impulsion sur `Velocity` du mob) + i-frames.
   Distinguer « frapper un mob » de « casser un bloc » selon ce que vise le raycast (mob le plus proche
   sur le rayon avant le premier bloc solide).
6. **Câblage** : `HostileSpawnSystem` + extension `MobAiSystem` dans le simScheduler.

## Contraintes
- A* **borné** (budget de nœuds + cooldown de recalcul par mob) pour ne pas plomber la sim ; structures
  primitives, pas de `HashMap` dans la boucle de recherche.
- I-frames des deux côtés (joueur et mob) ; pas de drain multi-frames.
- Pas de magic number : portées de détection/attaque, seuil de lumière de spawn, dégâts, knockback,
  budget A* → constantes.

## Tests (JUnit 5)
- `PathFinderTest` : chemin trouvé sur terrain simple, contournement d'un mur, échec propre si
  inaccessible / budget dépassé, déterminisme.
- `HostileSpawnSystemTest` : spawn refusé en lumière élevée / dans le rayon proche / au-dessus du cap.
- `CombatTest` : épée inflige plus que la main ; knockback applique une impulsion ; i-frames bloquent le
  double coup.
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : la nuit / dans le noir, des hostiles apparaissent, pathfind vers le joueur (contournent les
  murs), attaquent au contact ; on peut les tuer à l'épée (knockback visible) ; les morts-vivants brûlent
  au grand jour.

## Commit
`STEP-31: Add hostile mobs with A* pathfinding, spawning and player combat`
