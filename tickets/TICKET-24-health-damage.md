# STEP-24 — Vie & dégâts

**Milestone :** C — Survie · **Priorité :** 🟠 Moyenne · **Dépend de :** STEP-10 (physique/collision), STEP-17 (HUD)

## Objectif
Donner une santé au joueur, infliger des dégâts de **chute**, de **noyade** et de **lave**, gérer la mort
et le respawn au point de spawn.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/CollisionSystem.java` — détection du sol, `Grounded`, `Velocity.y`
  d'impact ; source de la vitesse d'atterrissage pour les dégâts de chute.
- `src/main/java/org/example/components/Grounded.java`, `Velocity.java`, `Position.java`, `ColliderAABB.java`.
- `src/main/java/org/example/systems/PhysicsSystem.java` — intégration vitesse, terminal velocity.
- `src/main/java/org/example/world/WorldConstants.java` — `TERMINAL_VELOCITY`, `WATER_LEVEL`, `BLOCK_WATER` ;
  ajouter un `BLOCK_LAVA` si on veut la lave (sinon laisser pour plus tard, mentionné).
- `src/main/java/org/example/systems/BlockInteractionSystem.java` / `CollisionSystem.isSolid(...)` —
  comment interroger le bloc à une position monde (pour détecter tête sous l'eau / pieds dans la lave).
- `src/main/java/org/example/Main.java` — position de spawn (pour le respawn).

## Spécification
1. **Composants** : `record Health(int current, int max)` ; `record DamageImmunity(float seconds)`
   (i-frames après un coup) ; optionnel `record Dead()` (tag).
2. **Dégâts de chute** : à l'atterrissage (transition non-`Grounded` → `Grounded`), si la vitesse
   verticale d'impact dépasse `SAFE_FALL_SPEED`, infliger des dégâts proportionnels à l'excès. Capter la
   vitesse d'impact **avant** que `CollisionSystem` ne la remette à 0 (ordre des systèmes / champ mémorisé).
3. **Noyade** : si la cellule à hauteur des yeux (`Position.y + PLAYER_EYE_HEIGHT`) est de l'eau pendant
   plus de `BREATH_SECONDS`, infliger 1 dégât toutes les `DROWN_INTERVAL`. Composant `Breath(float air)`
   qui se vide sous l'eau et se recharge à l'air.
4. **Lave** (si `BLOCK_LAVA` ajouté) : contact → dégâts réguliers + i-frames courtes. Sinon documenter
   comme TODO et ne pas bloquer le step.
5. **`HealthSystem`** (simScheduler) : applique chute/noyade/lave, décrémente `DamageImmunity`,
   régénère la vie hors STEP-25 (option : régen lente si pas de dégât récent). À `current <= 0` → mort.
6. **Mort & respawn** : à la mort, réinitialiser `Position` au spawn (stocké en constante ou composant
   `SpawnPoint`), `Health` au max, `Velocity` à zéro. (Écran de mort minimal possible, sinon respawn direct.)
7. **HUD** : afficher des cœurs (réutiliser le rendu de quads du `HudSystem` STEP-17) reflétant `Health`.

## Contraintes
- I-frames pour éviter le drain multi-frames d'un même évènement.
- Pas de magic number : `SAFE_FALL_SPEED`, dégâts/excès, `BREATH_SECONDS`, `DROWN_INTERVAL`,
  `MAX_HEALTH`, immunité → constantes (`WorldConstants`).
- Logique de dégâts **pure et testable** (séparer le calcul du dégât de l'accès World/GL).

## Tests (JUnit 5)
- `FallDamageTest` : fonction pure « dégât selon vitesse d'impact » (0 sous le seuil, croissant au-delà).
- `HealthSystemTest` : noyade après `BREATH_SECONDS` retire de la vie à intervalle ; i-frames empêchent
  le double coup ; `current<=0` déclenche la mort/respawn (position et vie réinitialisées).
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : une grosse chute blesse (petite chute non) ; rester immergé vide l'air puis noie ; la mort
  respawn au point de départ avec la vie pleine ; les cœurs du HUD reflètent la vie.

## Commit
`STEP-24: Add player health with fall/drowning damage, death and respawn`
