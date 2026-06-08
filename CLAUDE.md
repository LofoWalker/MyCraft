# MyCraft — Moteur voxel ECS (Java 26)

## Contexte projet
Moteur voxel style Minecraft en Java 26 pur avec architecture ECS data-oriented.
Stack : LWJGL 3.3.4 (GLFW + OpenGL 3.3 Core), JOML 1.10.7, Maven.
Plateforme : Windows uniquement (natives LWJGL Windows).

## Commandes Maven (Windows)
```powershell
$env:JAVA_HOME = "C:\Users\tom19\.jdks\openjdk-26.0.1"
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
& $mvn -f pom.xml <goal>
```
Goals courants : `compile` · `test` · `exec:java -Dexec.mainClass=org.example.Main`

## Architecture ECS — Règles absolues
- `Entity` = simple `int` ID, **jamais** une classe avec état.
- `record` pour **tous** les composants (data-only, zéro logique).
- `System` = logique pure, stateless, opère sur des queries du `World`.
- **Zéro héritage gameplay** (`extends` interdit pour entités/composants).
- **Pas de HashMap dans les boucles chaudes** — structures contiguës (`int[]`, `float[]`, packed arrays).
- Virtual threads uniquement pour génération de chunks et mesh building.
- Séparer strictement simulation world et rendering data.

## Structure de packages cible
```
org.example/
  ecs/          — Entity, World, ComponentStore<T>, System, SystemScheduler
  components/   — records : Position, Velocity, Rotation, CameraComponent,
                  PlayerInput, ColliderAABB, ChunkComponent, VoxelChunkData, Gravity
  systems/      — InputSystem, MovementSystem, PhysicsSystem, CollisionSystem,
                  CameraSystem, WorldGenSystem, ChunkMeshingSystem, RenderSystem
  render/       — Shader, Mesh, Window
  world/        — constantes monde, IDs de blocs
  Main.java
```

## Ordre d'exécution des Systems (cible finale)
```
InputSystem → PhysicsSystem → CollisionSystem → MovementSystem
→ CameraSystem → WorldGenSystem → ChunkMeshingSystem → RenderSystem
```

## État d'avancement
| Step | Titre | État |
|------|-------|------|
| 0 | Setup projet & fenêtre GLFW/OpenGL | ✅ Done |
| 1 | Cœur ECS (Entity, World, ComponentStore, SystemScheduler) | ✅ Done |
| 2 | Components (records Java 26) | ✅ Done |
| 3 | Game loop & fixed timestep | ✅ Done |
| 4 | Rendu OpenGL de base + Caméra | ✅ Done |
| 5 | Input & contrôleur FPS (noclip) | ✅ Done |
| 6 | Données voxel & rendu d'un chunk | ✅ Done |
| 7 | Meshing de chunk (face culling) | ✅ Done |
| 8 | Génération procédurale (Perlin/Simplex) | ✅ Done |
| 9 | Physique & gravité | ⬜ Next |
| 10 | Collision AABB vs voxels | ⬜ |
| 11 | Chunks dynamiques (streaming + virtual threads) | ⬜ |
| 12 | Polish & perfs | ⬜ |

## Conventions clean code
- **Noms expressifs** — pas d'abréviations sauf conventions établies (`dt`, `dx`, `ebo`).
- **Méthodes courtes** — < 20 lignes, une responsabilité par méthode.
- **Pas de commentaires QUOI** — le code se lit. Commentaires POURQUOI uniquement (contrainte non-évidente, invariant subtil).
- **Pas de magic numbers** — constantes nommées dans `WorldConstants` ou un enum.
- **Tests JUnit 5** pour toute logique ECS : `World`, `ComponentStore`, queries.
- **Pas de `null`** dans l'API publique ECS — `Optional` ou exception explicite.

## Format des commits
```
STEP-N: description courte en impératif (anglais)
```
Exemples :
- `STEP-1: Add ECS World with packed component stores`
- `STEP-2: Add data-only component records`
