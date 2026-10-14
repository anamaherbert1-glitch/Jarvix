# JARVIX — connexions officielles aux réseaux sociaux

## État réel

Cette livraison ajoute un socle OAuth côté serveur, le stockage chiffré des jetons, une protection par secret d'administration, une liste de connexions et des brouillons avec confirmation explicite. **Aucun compte n'est connecté par cette modification.**

Les routes de confirmation enregistrent la confirmation, mais n'exécutent pas encore d'actions auprès des réseaux. Les adaptateurs officiels pour publier, lire les statistiques, répondre aux commentaires et envoyer des messages restent à développer et à tester réseau par réseau. Aucune action sensible n'est exécutée automatiquement.

## Architecture et sécurité

- L'APK Android ne contient aucun secret OAuth client ni jeton social.
- Le navigateur passe par les pages d'autorisation officielles.
- Le serveur valide un paramètre state à usage unique, utilise PKCE pour TikTok, X, YouTube/Google et LinkedIn, échange le code côté serveur, puis chiffre le jeton avec AES-GCM avant stockage SQLite.
- Les routes d'administration exigent l'en-tête Authorization: Bearer avec JARVIX_ADMIN_TOKEN.
- HTTPS est obligatoire pour les redirections OAuth. Utilise un hébergement privé, un pare-feu, des sauvegardes protégées et un gestionnaire de secrets. Ne commite jamais de vrais secrets.
- Les états OAuth expirent après 10 minutes.
- Si la clé de chiffrement est perdue, les jetons stockés ne pourront plus être déchiffrés.

## Démarrage

Python 3.11+ recommandé.

    cd server
    python -m venv .venv
    source .venv/bin/activate
    pip install -r requirements-social.txt

Sous Windows, active l'environnement virtuel avec .venv\\Scripts\\activate. Configure ensuite les variables depuis server/.env.social.example dans les paramètres secrets de l'hébergeur.

Pour un test local seulement :

    uvicorn social_oauth:app --host 127.0.0.1 --port 8787

En production, exécute derrière un reverse proxy HTTPS (Caddy/Nginx ou un hébergeur HTTPS). N'expose pas directement le serveur de développement sur Internet. L'APK Android n'est pas encore relié à ce backend : il faut une interface d'administration HTTPS pour démarrer OAuth et consulter les connexions.

## Variables secrètes à configurer

- JARVIX_PUBLIC_BASE_URL : URL HTTPS publique du backend, sans slash final.
- JARVIX_ADMIN_TOKEN : secret aléatoire d'au moins 32 caractères.
- JARVIX_TOKEN_ENCRYPTION_KEY : clé de 32 octets encodée en base64 URL-safe.
- META_GRAPH_VERSION : version Graph API configurée dans l'app Meta.
- FACEBOOK_APP_ID et FACEBOOK_APP_SECRET.
- INSTAGRAM_APP_ID et INSTAGRAM_APP_SECRET.
- TIKTOK_CLIENT_KEY et TIKTOK_CLIENT_SECRET.
- GOOGLE_CLIENT_ID et GOOGLE_CLIENT_SECRET.
- X_CLIENT_ID et X_CLIENT_SECRET.
- LINKEDIN_CLIENT_ID et LINKEDIN_CLIENT_SECRET.

Générer les secrets dans un environnement de confiance, jamais dans le dépôt :

    python -c "import secrets; print(secrets.token_urlsafe(48))"
    python -c "import base64,secrets; print(base64.urlsafe_b64encode(secrets.token_bytes(32)).decode().rstrip('='))"

## URL de redirection à enregistrer dans chaque portail développeur

Remplacez votre-backend.example.com par le domaine HTTPS réel du backend et inscrivez l'URL exacte dans chaque portail :

- Facebook Pages : /oauth/facebook/callback
- Instagram : /oauth/instagram/callback
- TikTok : /oauth/tiktok/callback
- YouTube/Google : /oauth/youtube/callback
- X : /oauth/x/callback
- LinkedIn : /oauth/linkedin/callback
- WhatsApp Business : /oauth/whatsapp/callback

L'URL complète est https://votre-backend.example.com suivi du chemin indiqué. Le domaine et le chemin doivent correspondre exactement.

## Capacités et limites par plateforme

| Réseau | Base préparée | Limites |
|---|---|---|
| Facebook | OAuth Meta et permissions Pages | Publication, commentaires et statistiques sur les Pages uniquement, selon les permissions et l'examen Meta |
| Instagram | Instagram Login OAuth | Compte professionnel requis pour les fonctionnalités professionnelles |
| TikTok | OAuth avec PKCE | Publication vidéo soumise aux scopes approuvés, à l'audit et aux règles TikTok |
| YouTube | OAuth Google avec PKCE | YouTube Data API, quotas et vérification Google |
| X | OAuth 2.0 avec PKCE | API, fonctions et éventuels frais selon le niveau d'accès |
| LinkedIn | OAuth avec PKCE | Publication membre via permission approuvée; analytics et organisations peuvent demander d'autres accès |
| WhatsApp | Démarrage OAuth Meta | WhatsApp Business Platform et Embedded Signup requis; pas d'accès à un compte personnel |
| Telegram | Pas d'OAuth utilisateur | La Bot API fonctionne avec un bot créé via @BotFather; elle ne connecte pas ton compte personnel |

Les scopes sont une demande initiale, pas une garantie d'accès. Les portails développeur évoluent; vérifie les noms de scopes, les versions d'API et les conditions de revue avant le déploiement.

## Routes

- GET /health : vérification technique.
- GET /api/social/providers : fournisseurs et capacités indicatives; protégé.
- GET /api/social/status : état des connexions sans révéler les jetons; protégé.
- GET /oauth/{provider}/start : prépare puis ouvre le flux OAuth; protégé par secret d'administration.
- GET /oauth/{provider}/callback : callback OAuth public, protégé par state à usage unique.
- POST /api/actions/draft : crée un brouillon pour publish_post, publish_video, reply_comment ou send_message.
- POST /api/actions/{id}/confirm : enregistre la confirmation mais n'exécute pas encore l'action.
- DELETE /api/social/{provider} : supprime le jeton stocké côté serveur.

Le point de départ OAuth est protégé par bearer token; une interface d'administration sécurisée doit appeler le serveur avec cet en-tête puis ouvrir l'URL d'autorisation retournée. Ne mets pas le token d'administration dans l'URL ni dans les journaux.

## Avant la mise en production

1. Configurer le domaine HTTPS, les credentials et les URL de callback.
2. Ajouter le rafraîchissement/révocation des jetons et les flux spécifiques (Instagram, WhatsApp Embedded Signup, Telegram Bot).
3. Développer les adaptateurs API officiels pour les statistiques, médias, commentaires et messages.
4. Ajouter une interface affichant le réseau, le compte et le contenu exact, avec confirmation avant chaque action sensible.
5. Ajouter les webhooks, quotas, reprises idempotentes, journal d'audit sans secrets et suppression conforme des données.
6. Tester refus, expiration, révocation, quotas et permissions sur des comptes de test.
7. Faire auditer le code avant d'y connecter des comptes réels.

Ce code est une base de développement; il nécessite une revue de sécurité et des tests avant utilisation réelle.
