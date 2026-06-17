# STEP-23 — Cycle jour/nuit

**Milestone :** C — Survie · **Priorité :** 🟠 Moyenne · **Dépend de :** STEP-21 (skylight)

## Objectif
Faire avancer le temps du monde : direction et couleur du soleil/lune dérivées de l'heure, et niveau de
skylight global qui baisse la nuit — le monde s'assombrit puis se rallume.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/SkySystem.java` — `SUN_DIRECTION` **fixe** + `uTime`/`uSunDir` ;
  c'est ici que le soleil devra bouger.
- `src/main/resources/shaders/sky.vert` / `sky.frag` — couleurs de ciel selon la direction du soleil.
- `src/main/java/org/example/world/LightEngine.java` (STEP-21) — skylight ; appliquer un **facteur global**
  d'heure côté shader plutôt que recalculer le champ.
- `src/main/java/org/example/components/` — convention des records ; où stocker l'heure.
- `src/main/java/org/example/Main.java` — où enregistrer un système d'avancement du temps (simScheduler).

## Spécification
1. **État du temps** : `record TimeOfDay(float dayFraction)` (0..1, 0 = aube) porté par une entité monde
   (singleton) **ou** un champ dédié. Une journée dure `DAY_LENGTH_SECONDS` (constante).
2. **`TimeSystem`** (simScheduler) : avance `dayFraction` par `dt / DAY_LENGTH_SECONDS` (wrap 0..1).
   Pur, stateless (lit/écrit le composant `TimeOfDay`).
3. **Direction solaire** : `SkySystem` calcule `sunDir` à partir de `dayFraction` (rotation autour de
   l'axe est-ouest), au lieu de la constante. La nuit, basculer sur une direction « lune ».
4. **Couleurs** : `sky.frag` interpole les couleurs (aube/jour/crépuscule/nuit) selon la hauteur du
   soleil ; étoiles optionnelles la nuit. Garder le shader simple et piloté par `uSunDir`/un `uDayFactor`.
5. **Skylight global** : dériver un facteur d'éclairement global `[NIGHT_LIGHT .. 1.0]` de la hauteur du
   soleil, passé en uniform au shader des chunks (`basic.frag`) et **multiplié** au skylight. Éviter de
   recalculer le `LightEngine` (le blocklight des torches reste constant).
6. **Câblage** : `SkySystem` lit `TimeOfDay` depuis le World (le passer/parcourir dans `update`).

## Contraintes
- Le facteur global ne doit affecter que le **skylight**, pas le blocklight (torches inchangées la nuit).
- Pas de magic number : `DAY_LENGTH_SECONDS`, `NIGHT_LIGHT`, seuils aube/crépuscule → constantes.
- Le système de temps est de la **simulation** (dt fixe), le rendu (couleurs) lit l'état sans l'écrire.

## Tests (JUnit 5)
- `TimeSystemTest` : `dayFraction` avance proportionnellement à `dt` et wrap correctement à 1→0.
- Test pur de la fonction « facteur de lumière global selon dayFraction » (max à midi, min à minuit,
  transitions monotones).
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : le soleil traverse le ciel, le ciel change de couleur (aube→jour→crépuscule→nuit) ; le monde
  s'assombrit la nuit puis se rallume ; les torches éclairent toujours la nuit.

## Commit
`STEP-23: Add day/night cycle with moving sun and global skylight factor`
