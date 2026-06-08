Lance la suite de tests Maven et rapporte les résultats.

1. Exécute les tests :
```powershell
$env:JAVA_HOME = "C:\Users\tom19\.jdks\openjdk-26.0.1"
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
& $mvn -f "C:\Users\tom19\IdeaProjects\mycraft\pom.xml" test 2>&1 | Select-Object -Last 30
```

2. Affiche le résumé : nombre de tests passés / échoués / ignorés.
3. Pour chaque test échoué, cite le nom du test, le message d'assertion et la stack trace pertinente (5 lignes max).
4. Si aucun test n'existe encore, rappelle que les tests JUnit 5 doivent couvrir : World, ComponentStore, queries ECS.
