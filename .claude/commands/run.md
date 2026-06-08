Lance le jeu MyCraft (fenêtre GLFW + OpenGL).

1. Compile d'abord si nécessaire :
```powershell
$env:JAVA_HOME = "C:\Users\tom19\.jdks\openjdk-26.0.1"
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
& $mvn -f "C:\Users\tom19\IdeaProjects\mycraft\pom.xml" compile 2>&1 | Select-Object -Last 5
```

2. Lance l'application :
```powershell
$env:JAVA_HOME = "C:\Users\tom19\.jdks\openjdk-26.0.1"
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
& $mvn -f "C:\Users\tom19\IdeaProjects\mycraft\pom.xml" exec:java "-Dexec.mainClass=org.example.Main" 2>&1 | Select-Object -Last 15
```

3. Rapporte si la fenêtre s'est ouverte ou si une exception s'est produite.
