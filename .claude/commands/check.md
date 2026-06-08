Audite le code Java modifié (ou le fichier passé en argument) pour le respect du clean code et des règles ECS du projet.

Fichier/dossier ciblé : $ARGUMENTS (si vide, audite tous les fichiers modifiés depuis le dernier commit)

## Critères d'audit (par ordre de priorité)

### 1. Règles ECS absolues
- [ ] `Entity` n'est qu'un `int` — pas de champs ni méthodes métier
- [ ] Tous les composants sont des `record` (ou classes data-only sans logique)
- [ ] Aucun `extends` dans la hiérarchie gameplay
- [ ] Aucune HashMap/TreeMap dans les méthodes appelées par le game loop
- [ ] Les Systems sont stateless (pas de champs mutables hormis les dépendances injectées)

### 2. Clean code
- [ ] Méthodes > 20 lignes → signaler + suggérer découpage
- [ ] Noms abrégés sans raison (`tmp`, `x2`, `helper`) → signaler
- [ ] Magic numbers non nommés → signaler avec suggestion de constante
- [ ] Commentaires qui décrivent le QUOI → signaler (supprimer)
- [ ] `null` retourné dans l'API publique → signaler (utiliser `Optional` ou lever exception)

### 3. Performance
- [ ] Allocations dans les boucles chaudes (new, stream, boxing) → signaler
- [ ] Accès non-contiguës (index aléatoires sur grands tableaux) dans les Systems → noter

## Format de réponse
Pour chaque problème trouvé :
```
[FICHIER:LIGNE] CATÉGORIE — description du problème
  → Suggestion : ...
```
Termine par un résumé : N problèmes critiques, M avertissements.
Si le code est propre sur tous les critères, dis-le clairement.
