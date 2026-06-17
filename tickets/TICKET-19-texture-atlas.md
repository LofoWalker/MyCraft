# STEP-19 — Atlas de textures

**Milestone :** B — Rendu fidèle · **Priorité :** 🔴 Haute · **Dépend de :** STEP-7 (meshing), STEP-12 (rendu)

## Objectif
Remplacer les couleurs unies des blocs par des textures lues dans un **atlas** PNG unique, avec des UV
par face (top / side / bottom), pour un rendu fidèle à la beta tout en gardant **un seul bind de
texture** par passe de chunks.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/render/Mesh.java` — `STRIDE = 6 * Float.BYTES`, attribs `glVertexAttribPointer`
  (loc 0 = position xyz, loc 1 = couleur rgb). **Format de vertex à étendre** (ajout UV).
- `src/main/java/org/example/systems/ChunkMeshingSystem.java` — `FLOATS_PER_VERTEX = 6`, `MeshBuilder.addFace(...)`,
  `blockFaceColor(...)`, `FACE_OFFSETS`, `FACE_TOP`. C'est ici qu'on écrit les UV par sommet.
- `src/main/java/org/example/world/BlockType.java` — `colorTop()`/`colorSide()` ; **ajouter** les indices
  de tuile d'atlas par face.
- `src/main/java/org/example/render/Shader.java` — uniforms dispo ; **ajouter** `setUniform1i` (sampler).
- `src/main/resources/shaders/basic.vert` / `basic.frag` — shaders des chunks à adapter (UV + sampler).
- `pom.xml` — vérifier que le module **`lwjgl-stb`** (stb_image) est présent ; l'ajouter sinon (mêmes
  version/natives que les autres modules LWJGL).

## Spécification
1. **`render/TextureAtlas.java`** (`AutoCloseable`) : charge un PNG via `stb_image`
   (`STBImage.stbi_load_from_memory` depuis les resources, `stbi_set_flip_vertically_on_load`),
   crée une texture GL (`glGenTextures`, `GL_TEXTURE_2D`), filtrage **`GL_NEAREST`** (look pixelisé),
   `GL_CLAMP_TO_EDGE`, mipmaps optionnels. Expose `bind(int unit)` et `close()`.
2. **Layout d'atlas** : grille fixe N×N tuiles (ex. 16×16 px par tuile, atlas 256×256). Méthode util
   `uvForTile(int tileX, int tileY) -> float[]{u0,v0,u1,v1}` (testable sans GL). Ajouter une légère
   marge interne (`UV_INSET`) pour éviter le bleeding entre tuiles.
3. **`BlockType`** : ajouter `int tileTop()`, `tileSide()`, `tileBottom()` (index linéaire de tuile)
   par bloc. Garder `colorTop/colorSide` pour le HUD/fallback.
4. **Format de vertex** : passer de 6 à **8 floats** (pos xyz + UV uv + … ). Décider du contenu :
   minimum `pos(3) + uv(2)`. Si on garde une teinte (grass/feuilles), ajouter une couleur de teinte
   compacte. Mettre à jour **`Mesh.STRIDE`**, les `glVertexAttribPointer`, **`FLOATS_PER_VERTEX`** dans
   `ChunkMeshingSystem` et `MeshBuilder`, et `buildCubeVertices()` (mesh test) en cohérence.
5. **Meshing** : dans `addFace(...)`, écrire les 4 UV de la tuile de la face (utiliser `tileTop/Side/Bottom`
   selon `face`). Les coins UV suivent l'ordre CCW des `FACE_OFFSETS`.
6. **Shaders** : `basic.vert` passe l'UV au fragment ; `basic.frag` échantillonne `sampler2D uAtlas`
   (`texture(uAtlas, vUV)`), multiplie par la teinte éventuelle. `RenderSystem` binde l'atlas sur l'unité 0
   et fait `setUniform1i("uAtlas", 0)`.
7. **Texture** : fournir `resources/textures/blocks.png` (atlas placeholder : herbe top/side, terre,
   pierre, bois top/side, feuilles, eau, fer, diamant). Générer un PNG simple si aucun n'existe.

## Contraintes
- **Un seul bind d'atlas** par frame pour tous les chunks (pas de bind par chunk).
- Zéro allocation par frame dans `RenderSystem` (déjà le cas — ne pas régresser).
- `GL_NEAREST` obligatoire (rendu voxel net). Pas de magic number : tailles de tuile/atlas et `UV_INSET`
  → constantes nommées (dans `TextureAtlas` ou `WorldConstants`).

## Tests (JUnit 5)
- `TextureAtlasTest` (pur, sans GL) : `uvForTile(...)` renvoie les bons coins pour quelques tuiles
  (coins, centre), respecte l'inset et reste dans [0,1].
- `ChunkMeshingSystemTest` : adapter aux 8 floats/vertex ; vérifier qu'une face top porte les UV de
  `tileTop()` et une face latérale ceux de `tileSide()`.
- `/test` complet sans régression (penser à `MeshTest` si le stride y est vérifié).

## Critères d'acceptation
- `/run` : blocs texturés (herbe verte sur le dessus + terre sur les côtés, pierre, bois, feuilles…),
  rendu net (pixelisé), pas de bleeding entre tuiles, un seul bind de texture.

## Commit
`STEP-19: Add texture atlas with per-face UVs and textured chunk shader`
