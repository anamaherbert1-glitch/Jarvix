# JARVIS Android

Prototype Android natif en Kotlin : commandes vocales en français, réponses parlées, ouverture de quelques applications, sélection de fichiers via le sélecteur Android et service de notifications activable manuellement.

## Construire l’APK depuis un téléphone
Le workflow GitHub Actions `.github/workflows/build-apk.yml` compile l’APK debug et publie l’artefact `jarvis-debug-apk`. Ouvrir l’onglet Actions, lancer le workflow, puis télécharger l’artefact après réussite.

## Confidentialité
- Le microphone est demandé uniquement lorsque l’utilisateur touche le bouton Parler.
- L’accès aux fichiers est limité aux fichiers sélectionnés par l’utilisateur.
- L’accès aux notifications doit être activé manuellement dans les paramètres Android. Le prototype ne transmet pas de notifications sur Internet.
- Les commandes inconnues ne sont pas envoyées à un modèle IA : aucun cerveau IA distant n’est encore configuré.
- Ne place aucune clé API dans le dépôt ou dans l’application Android.

Ce projet est un prototype non encore testé sur un appareil physique.