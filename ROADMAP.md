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

## ~~Step 7 — Meshing de chunk~~ ✅
**But : transformer les voxels en un seul mesh efficace.**
- `ChunkMeshingSystem` : **face culling** (ne générer que les faces exposées à de l'air), un mesh par chunk.
- (Optionnel/extension : greedy meshing).
- Gestion des bordures entre chunks (voisinage).

✅ *Critère : chunk rendu en 1 draw call, faces internes supprimées.*

---

## ~~Step 8 — Génération procédurale~~ ✅
**But : terrain type Minecraft.**
- `WorldGenSystem` : bruit **Perlin/Simplex** (height map) → remplit les `VoxelChunkData`.
- Couches : pierre / terre / herbe selon la hauteur.
- Seed déterministe.

✅ *Critère : terrain ondulé généré, pas plat.*

---

## ~~Step 9 — Physique & gravité~~ ✅
**But : le joueur tombe et subit la gravité.**
- `PhysicsSystem` : applique `Gravity` → intègre `Velocity` puis `Position` (semi-implicit Euler).
- Saut (espace) ajoute une impulsion verticale.

✅ *Critère : le joueur tombe ; le saut le propulse.*

---

## ~~Step 10 — Collision AABB vs voxels~~ ✅
**But : marcher sur le sol, ne pas traverser les blocs.**
- `CollisionSystem` : AABB du joueur (`ColliderAABB`) testée contre les blocs solides autour.
- Résolution axe par axe (swept ou par pénétration), met `Velocity.y=0` au sol.
- Désactiver le noclip du Step 5 → contrôleur FPS complet (WASD + jump + gravité + collision).

✅ *Critère : on marche sur le terrain, on bute sur les murs, on ne tombe pas à l'infini.*

---

## ~~Step 11 — Chunks dynamiques (streaming)~~ ✅
**But : monde "infini" autour du joueur.**
- Charger/décharger les chunks dans un rayon autour de la `Position` du joueur.
- **Virtual Threads (Java 26)** pour : génération de chunks + mesh building en arrière-plan.
- File de résultats thread-safe → upload GPU sur le thread principal (OpenGL = mono-thread).

✅ *Critère : on se déplace, de nouveaux chunks apparaissent, les anciens se déchargent, sans freeze.*

---

## ~~Step 12 — Polish & perfs (extensions)~~ ✅
**But : nettoyer et préparer le scaling.**
- Séparer clairement **simulation world** et **rendering data** (cf. règles ECS du prompt).
- Batching par chunk/région, éviter allocations dans les boucles chaudes.
- Frustum culling (ne pas rendre les chunks hors champ).
- (Extensions futures : casser/poser des blocs, types de blocs, lumière, sauvegarde monde.)

✅ *Critère : les chunks hors champ ne sont plus dessinés ; la boucle de rendu n'alloue plus par chunk.*

Réalisé :
- `render/Frustum.java` : 6 plans extraits du view-projection (JOML `FrustumIntersection`), réutilisé en place chaque frame (zéro allocation).
- `RenderSystem` : test AABB chunk vs frustum → cull des chunks hors champ ; matrices `viewProjection`/`model` réutilisées (plus de `new Matrix4f()` par chunk/frame).
- Tests `FrustumTest` (devant/derrière/au-delà du far/sur le côté/à cheval sur un bord).

---

# 🌍 Phase 2 — Vers la « beta »

> **But de la phase :** atteindre une boucle de jeu comparable à Minecraft beta (b1.7/1.8) :
> casser **et poser**, inventaire/craft, textures + lumière, survie (vie/faim), mobs, fluides,
> biomes, sauvegarde et son. On garde les **règles ECS** : composants = `record` data-only,
> systèmes = logique pure stateless, zéro héritage gameplay, pas de hashmap en boucle chaude,
> virtual threads réservés gen/meshing/IO.

> 🎫 **Tickets rédigés :** chaque step de la Phase 2 (13 → 37) dispose d'un ticket détaillé,
> prêt à être développé en autonomie par un agent IA, dans `tickets/` (index : `tickets/README.md`).

## Déjà fait depuis le Step 12 (hors tableau d'origine)
- **Worldgen avancée** (`worldgen/`) : `GenerationPipeline` à étages — `TerrainStage`, `CaveStage`,
  `WaterSettleStage`, `TreeStage`, `OreStage` (fer/diamant). ⚠️ Actuellement court-circuitée par
  `FlatTerrainStage` (monde plat temporaire) → **à réactiver (Step 13)**.
- **Ciel procédural** : `SkySystem` (soleil + nuages animés), direction de soleil **fixe**.
- **Casser des blocs** : `BlockInteractionSystem` (raycast voxel Amanatides-Woo), système de hits
  via `BlockType.hardness()` + `BlockBreakProgress`, overlay de progression (`BlockBreakOverlaySystem`).

## Écarts majeurs vs beta (vue d'ensemble)
| Domaine | Existant | Manque |
|---|---|---|
| Interaction | casser (raycast + hits) | **poser**, surbrillance du bloc visé, choix du bloc |
| Inventaire | — | hotbar, inventaire, drops, ramassage |
| Rendu | couleurs unies, face+frustum culling | **textures (atlas)**, **lumière** (sky/block), transparence eau, AO |
| Survie | — | vie, faim, dégâts (chute/noyade), respawn |
| Items | — | registre items, stacks, **craft**, outils, fourneau, coffre |
| Monde vivant | — | **mobs** (passifs/hostiles), IA/pathfinding, spawn |
| Simulation | gen statique | **fluides dynamiques**, gravité sable/gravier, biomes |
| Méta | — | **sauvegarde/chargement**, **son** (OpenAL), menus |

---

## Milestone A — Boucle d'interaction complète ✅ LIVRÉ (steps 13-18)
*Rendre le monde « jouable » : poser, viser, ramasser, et vrai terrain.*

### Step 13 — ✅ Réactiver la vraie génération
- Retirer/condition­ner `FlatTerrainStage`, rebrancher le pipeline complet dans `WorldGenSystem`.
- Vérifier collisions/streaming sur terrain accidenté (montagnes, eau, grottes).
- ✅ *Critère : spawn sur terrain ondulé avec arbres/minerais, pas de blocage de chunk.*

### Step 14 — ✅ Surbrillance du bloc visé
- Nouveau `BlockHighlightSystem` (rendu) : wireframe sur la cible du raycast (réutiliser le ray de
  `BlockInteractionSystem` — extraire le raycast dans un util partagé `world/VoxelRaycast`).
- ✅ *Critère : un contour suit le bloc regardé dans la portée.*

### Step 15 — ✅ Poser des blocs
- Étendre `PlayerInput` : `placeBlock` (clic droit). Calculer la **face** touchée (normale) dans le
  raycast → poser dans la cellule adjacente si vide et non en collision avec le joueur.
- `ChunkDirty` sur le chunk modifié (remeshing déjà géré).
- ✅ *Critère : clic droit pose le bloc courant sur la face visée ; pas de pose dans le joueur.*

### Step 16 — ✅ Inventaire & hotbar (données)
- Components : `Inventory(ItemStack[] slots)`, `Hotbar(int selectedSlot)`, `ItemStack(int itemId, int count)`.
- Molette/touches 1-9 → slot sélectionné ; le bloc posé (Step 15) = item du slot actif.
- ✅ *Critère (tests JUnit) : ajout/retrait/empilement d'items, sélection de slot.*

### Step 17 — ✅ HUD 2D (overlay ortho)
- `HudSystem` : passe orthographique (crosshair, barre de hotbar, slot sélectionné).
- Rendu de texte/icônes via un atlas bitmap (préfigure le Milestone B).
- ✅ *Critère : crosshair centré + hotbar visible reflétant l'inventaire.*

### Step 18 — ✅ Drops d'items & ramassage
- Casser un bloc spawn une **entité item** (`ItemEntity`, `Position`, `Velocity`, `ItemStack`,
  collider réduit) ; physique réutilisée.
- `ItemPickupSystem` : aimantation + collecte dans l'`Inventory` quand le joueur s'approche.
- ✅ *Critère : casser un bloc fait tomber un item ramassable qui remplit la hotbar.*

---

## Milestone B — Rendu fidèle (textures + lumière) ✅ LIVRÉ (steps 19-22)
*Le plus gros saut visuel.*

### Step 19 — ✅ Atlas de textures
- `render/TextureAtlas` (chargement PNG via `stb_image`), UV par face dans `BlockType`
  (top/side/bottom). Adapter le format de vertex du meshing (ajout UV) et le shader.
- ✅ *Critère : blocs texturés (herbe/terre/pierre/bois…), 1 seul texture bind.*

### Step 20 — ✅ Eau & transparence
- Pass translucide séparé (blend, depth-write off, tri arrière→avant par chunk), faces d'eau
  non cullées entre elles ; surface légèrement abaissée.
- ✅ *Critère : eau semi-transparente correcte, pas de z-fighting.*

### Step 21 — ✅ Moteur de lumière (skylight + blocklight)
- Champ de lumière par chunk (`byte[]`, 4 bits sky + 4 bits block), propagation **flood-fill BFS**
  recalculée à la modification de bloc, sur virtual threads comme le meshing.
- Le meshing écrit un niveau de lumière par face → assombrissement dans le shader.
- Source de bloc : torche (nouveau `BlockType`, light=14).
- ✅ *Critère : grottes sombres, torches éclairent un rayon, faces orientées différemment ombrées.*

### Step 22 — ✅ Ambient occlusion (smooth lighting)
- AO par sommet (coins de voxels) calculée au meshing → rendu « smooth lighting » beta.
- ✅ *Critère : coins/recoins légèrement assombris, aspect beta.*

---

## Milestone C — Survie (cycle, vie, faim) ✅ LIVRÉ (steps 23-25)

### Step 23 — ✅ Cycle jour/nuit
- `TimeOfDay` (composant ou singleton world) ; `SkySystem` pilote la direction du soleil + couleurs ;
  niveau de skylight global dérivé de l'heure.
- ✅ *Critère : soleil/lune se déplacent, le monde s'assombrit la nuit.*

### Step 24 — ✅ Vie & dégâts
- `Health(int current, int max)` ; dégâts de **chute** (depuis la vélocité d'impact), **noyade**
  (tête sous l'eau > délai), lave. Régénération si conditions (faim pleine, étape 25).
- Mort → écran/respawn au point de spawn.
- ✅ *Critère : grosse chute blesse, rester sous l'eau noie, la mort respawn.*

### Step 25 — ✅ Faim & nourriture (beta 1.8)
- `Hunger(int food, float saturation)` ; se vide à l'effort, régule la régen, affame à 0.
- Items consommables (pomme/viande) qui restaurent la faim.
- HUD : barres cœurs + nourriture.
- ✅ *Critère : la faim baisse, manger la restaure, faim à 0 fait perdre de la vie.*

---

## Milestone D — Items, craft & blocs fonctionnels ✅ LIVRÉ (steps 26-28)

### Step 26 — Registre d'items & outils
- `ItemRegistry` (id → type, max stack, durabilité), familles d'outils (pioche/hache/pelle/épée).
- Vitesse de minage & drops dépendant de l'outil/matériau (étendre la logique de hits actuelle) ;
  diamant ne droppe qu'avec pioche fer+.
- ✅ *Critère : pioche casse la pierre plus vite, mauvais outil = pas/peu de drop.*

### Step 27 — Craft (2×2 + table 3×3)
- Grille de craft dans l'inventaire (2×2) + bloc `CraftingTable` (3×3) ; `RecipeBook` (recettes
  shaped/shapeless) ; UI d'inventaire (écran modal, curseur libre).
- ✅ *Critère : planches→établi→outils, recettes shaped fonctionnent.*

### Step 28 — Fourneau & coffre
- `Furnace` (entrée/combustible/sortie, progression de cuisson, recettes de fonte) ;
  `Chest` (stockage persistant lié au bloc).
- ✅ *Critère : fondre minerai en lingot, stocker/récupérer dans un coffre.*

---

## Milestone E — Monde vivant (mobs) ✅ LIVRÉ (steps 29-31)

### Step 29 — Cadre entités mobiles + rendu
- Components mob : `MobType`, `Health`, IA (`AiState`), `PathTarget`. Rendu de modèles simples
  (boîtes texturées) via un `EntityRenderSystem`.
- ✅ *Critère : une entité de test se déplace et se rend dans le monde.*

### Step 30 — Mobs passifs + spawn
- Vache/cochon/mouton/poule : errance aléatoire, fuite, drops à la mort. Règles de spawn (lumière
  jour, surface herbe), cap de population par zone.
- ✅ *Critère : des animaux apparaissent de jour, errent, droppent en mourant.*

### Step 31 — Mobs hostiles + combat + IA
- Zombie/squelette/araignée/creeper : spawn à faible lumière, pathfinding A* vers le joueur,
  attaque au contact, combustion au jour pour les morts-vivants. Combat joueur (épée, knockback).
- ✅ *Critère : la nuit, des hostiles traquent et blessent ; on peut les tuer.*

---

## Milestone F — Simulation dynamique du monde ✅ LIVRÉ (steps 32-34)

### Step 32 — Fluides dynamiques
- Écoulement eau/lave (niveaux 0-7, propagation décrémentale, mise à jour en file d'updates de
  blocs), lave+eau→pierre/obsidienne.
- ✅ *Critère : casser un mur d'eau la fait couler et se niveler.*

### Step 33 — Gravité de blocs & ticks aléatoires
- Sable/gravier tombent ; système de **random tick** par chunk (croissance d'herbe, propagation,
  base pour cultures).
- ✅ *Critère : sable suspendu tombe ; la terre nue se recouvre d'herbe.*

### Step 34 — Biomes
- Cartes température/humidité (bruit basse fréquence) → palette de blocs, densité d'arbres, teinte
  d'herbe par biome (plaine, désert, forêt, montagne, océan).
- ✅ *Critère : transitions de biomes visibles, déserts sans herbe, forêts denses.*

---

## Milestone G — Persistance, audio & menus ✅ LIVRÉ (steps 35-37)

### Step 35 — Sauvegarde / chargement du monde
- Sérialisation des chunks modifiés (fichiers région), state joueur (pos/inventaire/vie), seed.
- IO sur virtual threads ; chargement à la demande, fusion gen ↔ disque.
- ✅ *Critère : quitter/relancer restitue le terrain modifié, l'inventaire et la position.*

### Step 36 — Audio (OpenAL)
- `audio/SoundEngine` (LWJGL OpenAL), sons positionnels (pas, casse/pose, mobs), musique d'ambiance.
- ✅ *Critère : sons spatialisés sur les actions clés.*

### Step 37 — Menus & modes de jeu
- Menu principal (nouveau/charger monde), pause, sélection **Survie/Créatif** (vol + ressources
  infinies en créatif).
- ✅ *Critère : créer/charger un monde depuis un menu, basculer de mode.*

---

## Suggestions d'ordonnancement
- **Prioritaire (jouabilité immédiate) :** Steps 13-18 (poser + inventaire + HUD + vrai terrain).
- **Impact visuel max :** Milestone B (textures puis lumière).
- **Steps les plus lourds :** 21 (lumière), 27 (craft), 31 (IA mobs), 35 (sauvegarde) — prévoir
  plus de temps et de tests.
- Chaque step reste **compilable, testé (JUnit pour la logique), commité** `STEP-N: …`.

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
