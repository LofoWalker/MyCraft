Implémente le prochain step du ROADMAP.

## Protocole

1. **Lis** `ROADMAP.md` et identifie le premier step sans ✅ (cherche `⬜` ou l'absence de `✅`).
2. **Annonce** : "Je vais implémenter Step N — [titre]" avec le critère de succès.
3. **Lis** les fichiers existants pertinents avant d'écrire quoi que ce soit.
4. **Implémente** en respectant TOUTES ces règles (tirées de CLAUDE.md) :
   - `Entity` = `int` ID uniquement
   - `record` pour tous les composants
   - Systems stateless, zéro logique dans les composants
   - Pas de HashMap dans les boucles chaudes
   - Méthodes < 20 lignes, noms expressifs
   - Pas de magic numbers (constantes nommées)
   - Pas de commentaires QUOI — uniquement POURQUOI si non-évident
5. **Ajoute** les tests JUnit 5 correspondants si le step introduit de la logique ECS.
6. **Compile** pour vérifier :
```powershell
$env:JAVA_HOME = "C:\Users\tom19\.jdks\openjdk-26.0.1"
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
& $mvn -f "C:\Users\tom19\IdeaProjects\mycraft\pom.xml" compile 2>&1 | Select-Object -Last 10
```
7. **Corrige** toute erreur de compilation avant de déclarer le step terminé.
8. **Résume** les fichiers créés/modifiés et confirme que le critère de succès est rempli.
9. **Ne commite pas** — laisse le soin à l'utilisateur de valider avant le commit.

## Rappel des packages
- `org.example.ecs` — World, Entity, ComponentStore, System, SystemScheduler
- `org.example.components` — records de composants
- `org.example.systems` — implémentations des Systems
- `org.example.render` — Shader, Mesh, Window
- `org.example.world` — WorldConstants, génération
