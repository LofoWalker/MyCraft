# STEP-36 — Audio (OpenAL)

**Milestone :** G — Audio · **Priorité :** 🟢 Basse · **Dépend de :** STEP-15 (casse/pose), STEP-29 (mobs) pour les sons d'action

## Objectif
Ajouter un moteur audio via **OpenAL** (LWJGL) : sons positionnels (pas, casse/pose, mobs) et musique
d'ambiance, synchronisés avec les actions de jeu.

## Contexte / fichiers à lire d'abord
- `pom.xml` — modules LWJGL ; **ajouter `lwjgl-openal`** (lib + natives Windows, même version que le reste).
- `src/main/java/org/example/Window.java` — modèle de ressource native `AutoCloseable` (init/close) à
  copier pour le device/contexte OpenAL.
- `src/main/java/org/example/Main.java` — création/fermeture des sous-systèmes (`try (...)`), ordre des
  systèmes ; où enregistrer un `AudioSystem` et fermer le `SoundEngine`.
- `src/main/java/org/example/systems/BlockInteractionSystem.java` — casse/pose : points d'émission de sons.
- `src/main/java/org/example/components/Position.java`, `Velocity.java`, `RenderCamera.java` — position/
  orientation de l'auditeur (joueur) et des sources.
- `src/main/java/org/example/systems/MovementSystem.java`, `Grounded.java` — détecter les pas.

## Spécification
1. **`audio/SoundEngine.java`** (`AutoCloseable`) : ouvre le device OpenAL (`ALC10`/`AL10`), crée le
   contexte, charge des sons (WAV/OGG via `stb_vorbis` pour l'OGG) en buffers, gère un **pool de sources**
   réutilisables. API : `play(SoundId, x,y,z, volume, pitch)`, `playGlobal(SoundId, ...)`,
   `setListener(x,y,z, forward, up, velocity)`.
2. **Registre de sons** : enum `SoundId` (BLOCK_BREAK, BLOCK_PLACE, STEP, HURT, MOB_AMBIENT…) → fichier
   ressource. Sons sous `resources/sounds/` (placeholder si absents). Charger une fois au démarrage.
3. **`AudioSystem`** (renderScheduler ou simScheduler de fin) : met à jour le **listener** depuis la
   caméra/joueur chaque frame, et **émet** les sons d'évènements de jeu :
   - casse/pose : déclenché par `BlockInteractionSystem` (via un évènement/flag transitoire) ;
   - pas : cadence selon la vitesse horizontale quand `Grounded` ;
   - dégâts/mobs : sur les évènements correspondants (STEP-24/30/31 si présents).
4. **Musique d'ambiance** : lecture occasionnelle d'une piste (volume bas, non positionnel), avec
   intervalle aléatoire entre morceaux.
5. **Émission d'évènements** : pour rester ECS-propre, les systèmes de gameplay posent des composants/flags
   transitoires (ex. `record SoundEvent(SoundId id, float x,y,z)`) consommés et nettoyés par l'`AudioSystem`,
   plutôt qu'un appel direct au moteur depuis la logique.

## Contraintes
- Device/contexte OpenAL fermés proprement (`AutoCloseable`, comme `Window`).
- **Pool de sources** réutilisé : pas d'allocation/`genSources` par son joué. Buffers chargés une fois.
- Pas de magic number : volumes, pitch, cadence des pas, distance d'atténuation → constantes.
- L'audio ne doit pas bloquer la boucle (chargement au démarrage ; lecture non bloquante).

## Tests (JUnit 5)
- Tests **sans device réel** (OpenAL non garanti en CI) : logique pure testable seulement — ex. cadence
  des pas selon la vitesse, sélection de `SoundId`, mapping `SoundEvent`→source. Encapsuler les appels AL
  derrière une interface mockable, ou marquer les tests nécessitant un device comme conditionnels.
- `/test` complet sans régression (les tests audio ne doivent pas échouer faute de device).

## Critères d'acceptation
- `/run` : casser/poser un bloc joue un son spatialisé (atténué avec la distance) ; les pas se font
  entendre en marchant ; une musique d'ambiance se déclenche par moments ; fermeture sans erreur OpenAL.

## Commit
`STEP-36: Add OpenAL sound engine with positional effects and ambient music`
