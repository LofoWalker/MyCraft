# STEP-17 — HUD 2D (crosshair + hotbar)

**Milestone :** A — Interaction · **Priorité :** 🟠 Moyenne · **Dépend de :** STEP-16

## Objectif
Afficher un overlay 2D : crosshair au centre + barre de hotbar montrant les items et le slot
sélectionné.

## Contexte / fichiers à lire d'abord
- `src/main/java/org/example/systems/SkySystem.java` — exemple de passe plein écran (VAO vide + shader),
  gestion `AutoCloseable`, accès au `Shader`.
- `src/main/java/org/example/render/Shader.java`, `Mesh.java` — création de mesh, uniforms dispo
  (`setUniformMatrix4f`, `setUniform3f`, `setUniform1f`). **Ajouter** `setUniform1i`/`setUniform2f`
  si nécessaire pour les textures/positions.
- `src/main/java/org/example/Window.java` — largeur/hauteur, `getAspectRatio()`.
- `src/main/java/org/example/components/Inventory.java`, `Hotbar.java`, `ItemStack.java` (STEP-16).
- `src/main/java/org/example/Main.java` — `renderScheduler` (le HUD doit être **dernier**).

## Spécification
1. **Passe orthographique** : `HudSystem` (renderScheduler, en dernier) configure une projection
   ortho en pixels écran (0,0 en haut-gauche), **depth test off**, **blend on**.
2. **Crosshair** : petit quad/croix au centre de l'écran (taille en constante `WorldConstants` ou
   constantes du système). Couleur fixe (blanc additif) au début.
3. **Barre de hotbar** : 9 cases en bas-centre ; surligner le slot `Hotbar.selectedSlot` ; afficher
   un aperçu d'item par case (au minimum un quad coloré via `BlockType.colorTop()`, idéalement une
   icône d'atlas une fois STEP-19 fait) et le `count` si > 1.
4. **Texte / chiffres** : implémenter un mini-rendu de chiffres via un atlas bitmap simple
   (`render/BitmapFont.java`) — police monospace 8×8 suffisante. Charger une texture police depuis
   `resources/textures/font.png` (créer un PNG basique si absent).
5. **Shaders** : `resources/shaders/hud.vert` / `hud.frag` (quad texturé + couleur de teinte).
   Le HUD ne doit **pas** être affecté par la caméra 3D.

## Contraintes
- Recalculer l'ortho au resize (sinon HUD figé sur la résolution initiale) — lire la taille via `Window`.
- Zéro allocation par frame dans la boucle de dessin du HUD (buffers réutilisés).
- HUD purement rendu : il **lit** l'ECS, n'écrit rien dans la simulation.

## Tests (JUnit 5)
- Logique de layout testable sans GL : `HudLayout` (positions/tailles des cases en fonction de la
  résolution) → test des coordonnées du slot sélectionné, du centrage du crosshair.

## Critères d'acceptation
- `/run` : crosshair centré ; barre de hotbar en bas reflétant l'inventaire ; le slot actif est
  visuellement surligné et suit la molette/touches 1-9 ; le HUD se replace correctement au resize.

## Commit
`STEP-17: Add 2D HUD with crosshair and hotbar overlay`
