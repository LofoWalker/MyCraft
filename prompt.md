
Tu es un expert en engine design, Java 26, ECS (Entity Component System) et moteurs voxel type Minecraft.

 Je veux que tu génères une **beta de moteur 3D voxel jouable en Java 26**, architecturée proprement en ECS data-oriented.

 ---

 # 🎯 Objectif

 Construire un prototype jouable avec :

 * Monde voxel généré procéduralement (terrain type Minecraft)
 * Vue FPS (WASD + souris)
 * Physique simple (gravité + collision AABB)
 * Chunks dynamiques
 * Architecture ECS stricte

 ---

 # 🧱 Architecture OBLIGATOIRE (ECS pur)

 ## Entity

 * Uniquement un `int` ou `long` ID
 * Aucune logique métier

 ---

 ## Components (POJO / records Java 26)

 Utiliser des `record` ou classes data-only :

 * `Position(float x, float y, float z)`
 * `Velocity(float x, float y, float z)`
 * `Rotation(float yaw, float pitch)`
 * `CameraComponent`
 * `PlayerInput`
 * `ColliderAABB(float w, float h, float d)`
 * `ChunkComponent`
 * `VoxelChunkData` (structure optimisée par chunk)
 * `Gravity`

 ---

 ## Systems (logique uniquement)

 Chaque system doit être indépendant et stateless :

 * `InputSystem` → capture clavier + souris
 * `MovementSystem` → applique velocity - position
 * `PhysicsSystem` → gravité + intégration
 * `CollisionSystem` → AABB vs voxels
 * `CameraSystem` → FPS rotation + view matrix
 * `WorldGenSystem` → génération chunks (Perlin/Simplex noise)
 * `ChunkMeshingSystem` → transforme voxels en mesh
 * `RenderSystem` → rendu OpenGL (ou abstraction simple)

 ---

 # 🌍 Monde voxel

 * Monde divisé en chunks (ex: 16×16×16 ou 32×32×32)
 * Génération procédurale type Perlin noise
 * Stockage optimisé :
    
       * soit `byte[]` par chunk (block IDs)
   * soit palette compressée
 * Chunks chargés/déchargés dynamiquement autour du joueur

 ---

 # 🎮 FPS Controller

 Implémenter :

 * WASD movement
 * Jump (space)
 * Gravity
 * Mouse look (yaw/pitch)
 * Collision AABB avec blocs solides

 ---

 # ⚙️ Java 26 specific constraints / optimisations

 * Utiliser `records` pour components
 * Utiliser `Virtual Threads` pour :
    
       * génération de chunks
   * mesh building
 * Préférer structures cache-friendly (arrays, primitive arrays)
 * Éviter toute POO profonde (pas d’héritage gameplay)

 ---

 # 🧠 ECS performance rules

 * Systems doivent itérer sur des collections contiguës
 * Pas de hashmap dans les loops critiques
 * Séparer :
    
       * simulation world
   * rendering data
 * Favoriser batching par chunk / région

 ---

 # 💻 Résultat attendu

 Génère :

 1. Structure complète du projet Java 26
 2. Code ECS minimal mais fonctionnel
 3. Implémentation FPS jouable
 4. Génération de terrain voxel
 5. Système de collision basique fonctionnel

 ---

 # 🚀 Important

 Le but n’est pas un moteur complet AAA, mais une **beta propre, extensible et architecturalement correcte ECS**, prête à scaler vers un Minecraft-like multithreadé.
