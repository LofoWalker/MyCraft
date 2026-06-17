# STEP-35 — Sauvegarde / chargement du monde

**Milestone :** G — Persistance · **Priorité :** 🔴 Haute (step lourd) · **Dépend de :** STEP-11 (streaming), STEP-16 (inventaire)

## Objectif
Persister sur disque les **chunks modifiés**, l'**état du joueur** (position/inventaire/vie) et le
**seed**, puis recharger pour restituer un monde tel qu'on l'a laissé. IO sur virtual threads.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/ChunkStreamingSystem.java` — chargement/déchargement des chunks,
  workers virtual threads, `submitGeneration(...)`, `applyChunk(...)`, `ChunkDirty`. Point d'intégration
  de la fusion **gen ↔ disque** et de la sauvegarde au déchargement.
- `src/main/java/org/example/components/VoxelChunkData.java` — le `byte[]` à sérialiser ; format compact.
- `src/main/java/org/example/worldgen/GenerationPipeline.java` — la génération sert de **fallback** pour
  les chunks jamais modifiés (on ne sauvegarde que les chunks touchés).
- `src/main/java/org/example/Main.java` — création du monde, `seed` (actuellement
  `ThreadLocalRandom.nextLong()`), état du joueur (Position/Inventory/Health).
- `src/main/java/org/example/components/` — `Position`, `Rotation`, `Inventory`, `Health`, `Hotbar`.

## Spécification
1. **Format de sauvegarde** (`io/WorldStorage.java`) : un dossier de monde
   `saves/<name>/` contenant :
   - `level.dat` (ou `.json`) : seed, position/rotation joueur, inventaire/hotbar, vie/faim, heure (STEP-23).
   - fichiers **région** regroupant plusieurs chunks (ex. 32×32 chunks par région), chaque chunk modifié
     sérialisé (block ids compressés — RLE ou GZIP). Index des chunks présents dans la région.
2. **Quoi sauver** : uniquement les chunks **modifiés** (flag « dirty/édité » distinct du `ChunkDirty` de
   remesh — un `ChunkModified` posé à la 1ʳᵉ édition). Les chunks jamais touchés sont régénérés au load
   (déterminisme du seed). Sauvegarde au **déchargement** d'un chunk modifié et à la fermeture du jeu.
3. **Chargement** : `submitGeneration(...)` devient « charger depuis disque si présent, sinon générer ».
   Toujours sur virtual threads (IO + gen hors thread principal) ; upload GPU sur le thread principal
   (inchangé). Fusionner proprement (un chunk disque ne repasse pas par le pipeline).
4. **État joueur** : au lancement, si `level.dat` existe, restaurer seed + joueur ; sinon nouveau monde
   (seed aléatoire) et écrire `level.dat`. Sauver l'état joueur à la fermeture.
5. **Fermeture propre** : flush de tous les chunks modifiés + `level.dat` dans `Main`/`GameLoop` à l'arrêt
   (réutiliser la fermeture `AutoCloseable` existante).

## Contraintes
- IO **exclusivement sur virtual threads** ; jamais de lecture/écriture disque sur le thread de rendu.
- Sérialisation compacte (compression) ; format **versionné** (champ version pour évolutivité).
- Pas de magic number : taille de région, version de format, chemins → constantes. Pas de `null` dans l'API.
- Thread-safety : écriture/lecture région coordonnées (pas deux workers sur la même région simultanément).

## Tests (JUnit 5)
- `WorldStorageTest` : round-trip d'un `VoxelChunkData` (write→read = identique), y compris chunk vide et
  chunk plein ; round-trip de `level.dat` (joueur/inventaire/seed). Utiliser un dossier temporaire JUnit.
- Test : un chunk non modifié n'est pas écrit (régénéré au load) ; un chunk modifié est restitué tel quel.
- `/test` complet sans régression.

## Critères d'acceptation
- `/run` : modifier le terrain (casser/poser), bouger, remplir l'inventaire, quitter, relancer →
  terrain modifié, position, vie et inventaire **restitués** ; les zones non modifiées identiques au seed ;
  pas de freeze (IO en arrière-plan).

## Commit
`STEP-35: Add world save/load with region files and player state persistence`
