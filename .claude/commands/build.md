Compile le projet MyCraft avec Maven et rapporte le résultat.

1. Lance la compilation via PowerShell :
```powershell
$env:JAVA_HOME = "C:\Users\tom19\.jdks\openjdk-26.0.1"
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
& $mvn -f "C:\Users\tom19\IdeaProjects\mycraft\pom.xml" compile 2>&1 | Select-Object -Last 20
```

2. Affiche clairement : ✅ BUILD SUCCESS ou ❌ BUILD FAILURE.
3. En cas d'échec, cite les erreurs de compilation exactes avec le fichier et la ligne.
4. Ne propose pas de corrections sauf si demandé.
