# 🚀 Deployment Guide: Civify Backend -> Dokploy

This guide covers how to set up your VPS, install Dokploy, and configure the "plug and play" deployment pipeline with GitHub Actions.

## Phase 1: VPS Initialization (Server Setup)

1.  **Get a VPS**: Ubuntu 24.04 or 22.04 is recommended.
2.  **Install Docker & Dokploy**: SSH into your server and run this command:
    ```bash
    curl -sSL https://dokploy.com/install.sh | sh
    ```
3.  **Access Dashboard**:
    *   Go to `http://YOUR_SERVER_IP:3000`
    *   Create your Admin account.

---

## Phase 2: Create Application in Dokploy

We will configure Dokploy to receive the Docker Image built by GitHub Actions.

1.  **Create Project**: Click "Create Project" (e.g., "Civify").
2.  **Add Service**: Click "Create Service" -> Choose **"Application"**.
3.  **Configuration**:
    *   **Name**: `backend`
    *   **Build Type**: Choose **"Docker Image"**.
    *   **Image Name**: `ghcr.io/YOUR_GITHUB_USERNAME/civify:latest` (Replace `YOUR_GITHUB_USERNAME` with your actual username, lowercase).
    *   **Docker Registry**:
        *   Click "Add Registry" -> Select "GitHub Container Registry".
        *   **Username**: Your GitHub Username.
        *   **Password**: **IMPORTANT**: Use a **Personal Access Token (PAT)**, NOT your login password.
            *   Go to GitHub -> Settings -> Developer Settings -> Personal access tokens -> Tokens (classic).
            *   Generate new token -> Select scope `read:packages` (This is the secure way; it allows Dokploy to *only* read images).
            *   Paste this token as the password.

4.  **Deployment Webhook**:
    *   Go to the **"Deployment"** tab inside the application settings.
    *   Copy the **Webhook URL**. You will need this for GitHub Actions.

---

## Phase 3: Environment Variables (Database & Secrets)

**Crucial Step**: Since we are not putting `.env` files in the Docker image (for security), you must set them in Dokploy.

1.  Go to the **"Environment"** tab in your Application.
2.  Add your variables. For a Spring Boot app, they typically look like this:

| Variable Name | Value Example | Notes |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/civify` | If using Dokploy's internal Postgres |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | |
| `SPRING_DATASOURCE_PASSWORD` | `your_secure_password` | |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates the `prod` profile |
| `JWT_SECRET` | `very_long_random_secret_string` | For your auth tokens |

> **Tip**: If you create a Postgres database inside Dokploy (Services -> Database -> Postgres), it will give you the "Internal Network" connection details. Use those!

---

## Phase 4: GitHub Actions Setup

1.  **Go to your GitHub Repository** -> Settings -> Secrets and Variables -> Actions.
2.  **New Repository Secret**:
    *   `DOKPLOY_WEBHOOK`: Paste the URL you copied from Dokploy in Phase 2.
    *   *(Note: We don't need DOCKER_USERNAME/PASSWORD secrets because the workflow uses the automatic `GITHUB_TOKEN` to push to the registry).*

---

## Phase 5: Deploy!

1.  Push your code to the `staging` branch.
2.  Go to the **"Actions"** tab in GitHub to watch the build.
3.  Once the "Build and push" and "Trigger Dokploy" steps complete, check your Dokploy logs.
4.  Your app should start up!

---

## 🔒 Security Note: Why PAT and not SSH?

You asked about using SSH. Here is why we use a PAT (Personal Access Token) for this specific setup:

*   **The Workflow**: We are building the image **on GitHub** (to save your VPS CPU/RAM) and pushing the *finished artifact* (the Docker image) to the registry.
*   **Dokploy's Role**: Dokploy simply *downloads* (pulls) this image. It does not clone your git repo.
*   **Authentication**: Docker Registries (like GHCR) use Token-based authentication, not SSH keys.
*   **Security**: By creating a PAT with **only** the `read:packages` scope, you ensure that even if the token is leaked, an attacker can only *download* your public/private images. They **cannot** change your code, push malicious code, or access your other repositories. This is the industry standard for secure CI/CD image pulls.
