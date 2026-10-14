# JARVIX Android + local server

Prototype Android natif : commandes vocales, réponses parlées, ouverture de quelques applications, sélection de fichiers et notifications après autorisation.

## Serveur téléphone
1. Compile l'APK via GitHub Actions et installe-le.
2. Mets les appareils sur le même Wi-Fi.
3. Dans JARVIX, touche « DÉMARRER / ARRÊTER LE SERVEUR DU TÉLÉPHONE ».
4. L'application affiche l'adresse locale et une clé Bearer. Garde cette clé privée.
5. API : GET /health; POST /v1/command avec Authorization: Bearer <clé> et JSON {"command":"..."}.

Le serveur Android est un prototype et ne fonctionne que tant que le processus de l'application reste actif. Android peut suspendre l'application en arrière-plan.

## Serveur ordinateur
Le fichier server/server.py utilise Python standard. Installe Python 3 et définis une clé de 24 caractères minimum. Par défaut il écoute uniquement sur 127.0.0.1. Pour le réseau local de confiance, définis JARVIX_HOST=0.0.0.0 et limite les connexions avec le pare-feu. Ne l'expose pas directement à Internet.

## Connexions sociales officielles
Le socle OAuth serveur et la documentation sont dans server/social_oauth.py et SOCIAL_INTEGRATIONS.md. Il utilise les pages OAuth officielles et chiffre les jetons stockés. Aucun compte n'est connecté automatiquement, aucun mot de passe social n'est demandé, et les actions de publication/messages ne sont pas encore exécutées par les adaptateurs API. Il faut configurer les applications développeur, les secrets et un domaine HTTPS; voir SOCIAL_INTEGRATIONS.md.

## Cerveau IA
Le backend comprend `server/ai_brain.py` : API de conversation protégée, clé IA conservée côté serveur, historique non stocké, et garde-fou qui signale les demandes d'actions sensibles. Configure `OPENAI_API_KEY` et `OPENAI_MODEL` dans les secrets de l'hébergement. Le cerveau n'est pas encore branché à l'interface Android; aucune publication ou aucun message social n'est exécuté. Voir `SOCIAL_INTEGRATIONS.md`.

## Limites
- Le serveur ordinateur et l'application Android ne sont pas encore reliés automatiquement.
- Le serveur local reçoit les commandes mais n'exécute aucune commande système arbitraire.
- Les permissions microphone, fichiers et notifications restent soumises aux autorisations Android.
- Code en développement : vérifie GitHub Actions et teste sur appareil avant usage réel.
