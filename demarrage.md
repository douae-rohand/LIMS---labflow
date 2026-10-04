# Démarrer LabFlow

MySQL, la base `lims_central`, `backend/.env`, `frontend/.env` et `npm install` sont déjà en place. Deux terminaux suffisent.

## 1. Backend

Depuis la racine du dépôt (`LIMS---labflow`). Maven se place ensuite dans `backend/` et lit `backend/.env`.

```powershell
.\backend\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Ne pas activer le profil `dev` : il désactive Flyway.

- API : http://localhost:8081
- Swagger : http://localhost:8081/swagger-ui.html

Attendre la fin du démarrage avant d’ouvrir le frontend. Pour arrêter le backend, utiliser `Ctrl+C` dans son terminal.

Si le démarrage s’arrête sur `Port 8081 was already in use`, un ancien backend tourne encore. Le trouver et l’arrêter :

```powershell
Get-NetTCPConnection -LocalPort 8081 -State Listen | Select-Object -ExpandProperty OwningProcess | ForEach-Object { Stop-Process -Id $_ -Force }
```

## 2. Frontend

Dans un second terminal :

```powershell
cd frontend
npm run dev
```

L’interface est sur http://localhost:5173. Les appels `/api` partent vers http://localhost:8081.

## Ordre

1. Backend
2. Frontend
