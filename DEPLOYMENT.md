# Deployment (CI/CD)

Every push to `master` triggers `.github/workflows/deploy.yml`, which runs two
independent jobs:

| Job                | What it does                                                        |
| ------------------ | ------------------------------------------------------------------- |
| `deploy-backend`   | Builds the Spring Boot JAR, copies it to the GCP VM over SSH, and rebuilds/restarts the Docker container. |
| `deploy-frontends` | Builds both Angular apps (production) and deploys them to Firebase Hosting (`citizen` + `admin` targets). |

You can also trigger it manually from the **Actions** tab → **Deploy** → **Run workflow**.

---

## 1. Create the GCP Compute Engine instance

Run these locally with the `gcloud` CLI (or use the Cloud Console UI). Replace
`YOUR_PROJECT` and adjust zone/machine type as needed.

```bash
# Pick your project
gcloud config set project YOUR_PROJECT

# Create an Ubuntu 22.04 VM (e2-small is enough for a small deployment)
gcloud compute instances create praja-disha \
  --zone=asia-south1-a \
  --machine-type=e2-small \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=20GB \
  --tags=http-server,https-server

# Allow inbound traffic on 8080 (the backend port)
gcloud compute firewall-rules create allow-backend-8080 \
  --allow=tcp:8080 \
  --target-tags=http-server \
  --description="Praja Disha backend"
```

Note the VM's **external IP** (`gcloud compute instances list`) — you'll need it
for the `VM_HOST` secret.

## 2. Install Docker on the VM

SSH in (`gcloud compute ssh praja-disha --zone=asia-south1-a`) and run:

```bash
# Install Docker Engine + compose plugin
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
# log out and back in so the group change takes effect
```

Verify: `docker compose version`.

## 3. Create a deploy SSH key (used by GitHub → VM)

On your local machine:

```bash
ssh-keygen -t ed25519 -f praja-deploy-key -C "github-actions" -N ""
```

This produces `praja-deploy-key` (private) and `praja-deploy-key.pub` (public).

Add the **public** key to the VM's authorized keys:

```bash
gcloud compute ssh praja-disha --zone=asia-south1-a
# then on the VM:
echo "PASTE_CONTENTS_OF_praja-deploy-key.pub" >> ~/.ssh/authorized_keys
```

> The VM user that owns this key is your `VM_USER` (e.g. your GCP username).

## 4. Create the backend `.env` on the VM (once)

The container reads its config from `~/praja-disha/.env`. Create it on the VM —
**never commit this file**:

```bash
mkdir -p ~/praja-disha
cat > ~/praja-disha/.env <<'EOF'
JWT_SECRET=replace-with-a-long-random-string-at-least-32-bytes
JWT_EXPIRATION_DAYS=365

OLLAMA_ENABLED=true
PUBLIC_BASE_URL=http://YOUR_VM_IP:8080

# GCP storage (leave blank to use local disk uploads)
GCP_BUCKET_NAME=
GCP_PROJECT_ID=
GCP_CREDENTIALS_PATH=
EOF
```

> `MONGODB_URI` and `OLLAMA_API_KEY` are **not** in this file — they come from
> GitHub secrets and are injected into the container at deploy time (they
> override anything in `.env`). Everything else lives here on the VM.
>
> The workflow only copies the app bundle, never `.env`, so your secrets stay on
> the VM. Update this file directly whenever a value changes.

## 5. Get the Firebase CI token

The `deploy-frontends` job authenticates with a Firebase CI token:

```bash
firebase login:ci
```

Copy the token it prints — that's the `FIREBASE_TOKEN` secret. The Firebase
project (`praja-disha`) and hosting targets are already configured in
`frontend/.firebaserc`.

## 6. Add the GitHub secrets

Repo → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret           | Value                                                        |
| ---------------- | ----------------------------------------------------------- |
| `VM_HOST`        | The VM's external IP (from step 1).                         |
| `VM_USER`        | The SSH username on the VM (owner of the deploy key).       |
| `VM_SSH_KEY`     | Full contents of the **private** key `praja-deploy-key`.    |
| `MONGODB_URI`    | Your MongoDB Atlas connection string.                       |
| `OLLAMA_API_KEY` | Your Ollama API key.                                        |
| `FIREBASE_TOKEN` | Token from `firebase login:ci` (step 5).                    |

## 7. Deploy

Push to `master` (or run the workflow manually). To confirm the backend:

```bash
gcloud compute ssh praja-disha --zone=asia-south1-a
docker ps                       # praja-disha-backend should be running
docker logs -f praja-disha-backend
curl http://localhost:8080/     # from the VM
```

From anywhere: `http://YOUR_VM_IP:8080`.

---

## Notes / next steps

- **Frontend API URL:** `frontend/*/src/environments/environment.ts` currently
  points `apiBaseUrl` at `http://localhost:8080`. For the deployed frontends to
  reach the VM, change this to `http://YOUR_VM_IP:8080` (or a domain) and
  redeploy. Ideally put a TLS reverse proxy (Nginx/Caddy) in front of the VM and
  use `https://api.yourdomain`.
- **MongoDB:** the compose file assumes MongoDB Atlas via `MONGODB_URI`. To run
  Mongo on the VM instead, uncomment the `mongo` service in
  `backend/docker-compose.prod.yml` and set `MONGODB_URI=mongodb://mongo:27017/praja_disha`.
- **HTTPS:** port 8080 is plain HTTP. For production, front it with a reverse
  proxy terminating TLS (Let's Encrypt) and open 443 instead of 8080.
