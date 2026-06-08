# 🗺️ Roadmap — Moteur voxel ECS (mycraft)

Découpage du `prompt.md` en steps incrémentaux. **Chaque step produit quelque chose de
compilable et testable** : on n'avance au suivant que quand le précédent tourne.

État de départ : projet Maven Java 26 vide (`Main.java` par défaut), aucune dépendance.

---

## ~~Step 0 — Setup projet & fenêtre~~ ✅
**But : une fenêtre noire OpenGL qui s'ouvre et se ferme proprement.**
- Ajouter les dépendances dans `pom.xml` : **LWJGL 3** (GLFW + OpenGL) et **JOML** (maths : vecteurs/matrices).
- Configurer les natives LWJGL pour Windows.
- Créer une classe `Window` (init GLFW, contexte OpenGL, swap buffers, vsync).
- Boucle minimale : `glClear` + `glfwSwapBuffers` + `glfwPollEvents`.

✅ *Critère : `mvn compile` passe, une fenêtre s'affiche.*

---

## Step 1 — Cœur ECS
**But : le framework ECS data-oriented, sans gameplay.**
- `Entity` = simple `int` ID (générateur d'IDs, recyclage).
- `World` / registre : stockage des composants par **type**, en tableaux contigus (pas de hashmap dans les boucles chaudes — au pire `Map<Class, ComponentStore>` hors boucle).
- `ComponentStore<T>` : stockage dense (packed array) + mapping entity→index.
- Interface `System` (méthode `update(World, float dt)`), et un `SystemScheduler` qui les exécute dans l'ordre.
- Itération sur collections contiguës (vues/queries par archétype ou par composant).

✅ *Critère : créer N entités, ajouter/retirer des composants, itérer dessus dans un test.*

---

## ~~Step 2 — Components (records Java 26)~~ ✅
**But : tous les composants data-only du prompt.**
- `Position(float x,y,z)`, `Velocity(float x,y,z)`, `Rotation(float yaw, pitch)`
- `CameraComponent`, `PlayerInput`, `Gravity`
- `ColliderAABB(float w,h,d)`
- `ChunkComponent`, `VoxelChunkData` (cf. Step 6)
- Tous en `record` (immutables) ou classes data-only mutables si la perf l'exige.

✅ *Critère : composants attachables à une entité via le World du Step 1.*

---

## ~~Step 3 — Game loop & timestep~~ ✅
**But : boucle de jeu propre.**
- Fixed timestep pour la simulation (physique déterministe), rendu découplé.
- Calcul du `dt`, accumulateur, FPS counter.
- Le scheduler appelle les systems dans l'ordre : Input → Physics → Collision → Camera → (World/Mesh) → Render.

✅ *Critère : boucle stable à ~60 FPS, dt mesuré.*

---

## ~~Step 4 — Rendu OpenGL de base + Caméra~~ ✅
**But : afficher un cube en 3D avec une caméra perspective.**
- `Shader` (compile/link vertex+fragment, uniforms).
- `Mesh` (VAO/VBO/EBO), rendu d'un cube test.
- `CameraSystem` : construit la **view matrix** (depuis Position + Rotation) et la **projection** (perspective JOML).
- `RenderSystem` minimal : upload matrices, draw call.

✅ *Critère : un cube texturé/coloré visible, projection correcte.*

---

## ~~Step 5 — Input & contrôleur FPS (sans collision)~~ ✅
**But : voler librement dans la scène.**
- `InputSystem` : capture clavier (WASD, espace) + souris (delta) via GLFW callbacks, curseur capturé.
- Écrit dans `PlayerInput`.
- `MovementSystem` : applique l'input → `Velocity` → `Position` (mode noclip pour l'instant).
- Mouse look → met à jour `Rotation` (yaw/pitch, clamp pitch).

✅ *Critère : on se déplace et on regarde autour à la souris.*

---

## ~~Step 6 — Données voxel & un chunk~~ ✅
**But : structure de stockage d'un chunk + rendu d'un chunk plein.**
- `VoxelChunkData` : `byte[]` de block IDs (ex. 16×16×16 ou 32³), indexation `x + y*S + z*S*S`.
- Constantes monde (taille chunk, IDs de blocs : air, terre, herbe, pierre…).
- `ChunkComponent` (coordonnées du chunk dans le monde).
- Remplir un chunk à la main et le rendre (cube par bloc, naïf au départ).

✅ *Critère : un chunk de blocs visible à l'écran.*

---

## Step 7 — Meshing de chunk
**But : transformer les voxels en un seul mesh efficace.**
- `ChunkMeshingSystem` : **face culling** (ne générer que les faces exposées à de l'air), un mesh par chunk.
- (Optionnel/extension : greedy meshing).
- Gestion des bordures entre chunks (voisinage).

✅ *Critère : chunk rendu en 1 draw call, faces internes supprimées.*

---

## Step 8 — Génération procédurale
**But : terrain type Minecraft.**
- `WorldGenSystem` : bruit **Perlin/Simplex** (height map) → remplit les `VoxelChunkData`.
- Couches : pierre / terre / herbe selon la hauteur.
- Seed déterministe.

✅ *Critère : terrain ondulé généré, pas plat.*

---

## Step 9 — Physique & gravité
**But : le joueur tombe et subit la gravité.**
- `PhysicsSystem` : applique `Gravity` → intègre `Velocity` puis `Position` (semi-implicit Euler).
- Saut (espace) ajoute une impulsion verticale.

✅ *Critère : le joueur tombe ; le saut le propulse.*

---

## Step 10 — Collision AABB vs voxels
**But : marcher sur le sol, ne pas traverser les blocs.**
- `CollisionSystem` : AABB du joueur (`ColliderAABB`) testée contre les blocs solides autour.
- Résolution axe par axe (swept ou par pénétration), met `Velocity.y=0` au sol.
- Désactiver le noclip du Step 5 → contrôleur FPS complet (WASD + jump + gravité + collision).

✅ *Critère : on marche sur le terrain, on bute sur les murs, on ne tombe pas à l'infini.*

---

## Step 11 — Chunks dynamiques (streaming)
**But : monde "infini" autour du joueur.**
- Charger/décharger les chunks dans un rayon autour de la `Position` du joueur.
- **Virtual Threads (Java 26)** pour : génération de chunks + mesh building en arrière-plan.
- File de résultats thread-safe → upload GPU sur le thread principal (OpenGL = mono-thread).

✅ *Critère : on se déplace, de nouveaux chunks apparaissent, les anciens se déchargent, sans freeze.*

---

## Step 12 — Polish & perfs (extensions)
**But : nettoyer et préparer le scaling.**
- Séparer clairement **simulation world** et **rendering data** (cf. règles ECS du prompt).
- Batching par chunk/région, éviter allocations dans les boucles chaudes.
- Frustum culling (ne pas rendre les chunks hors champ).
- (Extensions futures : casser/poser des blocs, types de blocs, lumière, sauvegarde monde.)

---

## Ordre d'exécution des systems (cible)
```
InputSystem → PhysicsSystem → CollisionSystem → MovementSystem
→ CameraSystem → WorldGenSystem → ChunkMeshingSystem → RenderSystem
```

## Règles transverses (rappel du prompt)
- `record` pour les components, **aucune logique** dedans.
- **Aucun héritage gameplay** (pas de POO profonde).
- Pas de hashmap dans les boucles critiques ; structures cache-friendly (arrays primitifs).
- Virtual threads pour génération + meshing uniquement.
- Objectif = **beta propre et extensible**, pas un AAA.
