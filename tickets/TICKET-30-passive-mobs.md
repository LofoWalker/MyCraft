# STEP-30 — Mobs passifs + spawn

**Milestone :** E — Monde vivant · **Priorité :** 🟠 Moyenne · **Dépend de :** STEP-29

## Objectif
Faire vivre des animaux passifs (vache/cochon/mouton/poule) : errance aléatoire, fuite quand frappés,
drops à la mort, et règles de spawn (de jour, sur l'herbe, avec un cap de population par zone).

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/components/MobType.java`, `AiState.java`, `PathTarget.java`, `world/Mobs.java`
  (STEP-29) — socle mob + spawn util.
- `src/main/java/org/example/systems/MovementSystem.java`, `PhysicsSystem.java` — appliquer la vélocité
  d'errance ; le saut pour franchir un bloc.
- `src/main/java/org/example/systems/CollisionSystem.java`, `isSolid(...)` — détecter le sol/obstacles
  pour l'errance et la validité d'un point de spawn.
- `src/main/java/org/example/systems/ItemPickupSystem.java` / drops (STEP-18) — réutiliser le spawn de
  drops à la mort.
- `src/main/java/org/example/components/Health.java` (STEP-24), `TimeOfDay` (STEP-23) — fuite/mort, règle
  jour.
- `src/main/java/org/example/systems/ChunkStreamingSystem.java` — savoir quels chunks sont chargés (spawn
  uniquement dans les chunks actifs).

## Spécification
1. **IA d'errance** (`MobAiSystem`, simScheduler) : machine à états simple par `AiState` —
   IDLE (attente aléatoire) → WANDER (choisir un `PathTarget` proche, marcher vers lui, sauter sur
   obstacle d'1 bloc) → retour IDLE. FLEE quand récemment blessé (s'éloigner de la source un court délai).
   Logique de steering **pure et testable** (vecteur vers la cible → vélocité horizontale).
2. **Drops à la mort** : à `Health <= 0`, spawner les drops du mob (table `mobDrops(MobType)`, ex.
   vache→cuir/bœuf) via le système de drops, puis `world.destroy(mob)`.
3. **Règles de spawn** (`PassiveSpawnSystem`, simScheduler, throttlé) : périodiquement, tenter de spawner
   un petit groupe sur des blocs **d'herbe** exposés au ciel, **de jour** (`TimeOfDay`), dans les chunks
   chargés autour du joueur mais hors d'un rayon minimal. Respecter un **cap de population** par zone/chunk
   (compter les mobs existants).
4. **Cap & despawn** : ne pas dépasser `MAX_PASSIVE_PER_AREA` ; despawn des mobs trop loin du joueur
   (au-delà du rayon de chunks chargés) pour borner le coût.
5. **Câblage** : enregistrer `MobAiSystem` et `PassiveSpawnSystem` dans le simScheduler ; `EntityRenderSystem`
   (STEP-29) les rend déjà.

## Contraintes
- Pas de pathfinding lourd ici (errance locale + saut simple) ; A* réservé aux hostiles (STEP-31).
- Spawn/despawn throttlés, sans balayage coûteux par frame ; pas de `HashMap` dans les boucles serrées
  (compter via les queries ECS).
- Pas de magic number : intervalles, rayons, `MAX_PASSIVE_PER_AREA`, vitesses d'errance → constantes.

## Tests (JUnit 5)
- `MobAiSystemTest` : depuis WANDER avec un `PathTarget`, la vélocité pointe vers la cible ; FLEE éloigne
  de la source ; transition IDLE↔WANDER selon timers.
- `PassiveSpawnSystemTest` : refuse de spawner la nuit / hors herbe / au-dessus du cap ; accepte dans les
  conditions valides (en mockant World/heure).
- `MobDropsTest` : table de drops correcte par `MobType`.
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : des animaux apparaissent de jour sur l'herbe, errent et sautent les petits obstacles, fuient
  quand on les frappe, droppent des items à la mort ; la population reste bornée.

## Commit
`STEP-30: Add passive mobs with wandering AI, spawn rules and death drops`
