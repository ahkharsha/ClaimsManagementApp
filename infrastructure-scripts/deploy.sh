#!/bin/bash
# ============================================
# InsuranceIQ — EC2 Deployment Script
# ============================================
# Run this on a fresh Ubuntu EC2 instance
# Usage: chmod +x deploy.sh && ./deploy.sh

set -e

echo "╔══════════════════════════════════════════╗"
echo "║  InsuranceIQ — EC2 Deployment            ║"
echo "╚══════════════════════════════════════════╝"

# ── Step 1: Install Docker ──
echo "[1/5] Installing Docker..."
if ! command -v docker &> /dev/null; then
    sudo apt-get update
    sudo apt-get install -y apt-transport-https ca-certificates curl software-properties-common
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    sudo usermod -aG docker $USER
    echo "Docker installed. You may need to log out and back in for group changes."
else
    echo "Docker already installed."
fi

# ── Step 2: Build React frontend ──
echo "[2/5] Building React frontend..."
if command -v node &> /dev/null; then
    cd client
    npm install
    VITE_SPRING_API_URL=/api VITE_SOCKET_URL=/ npm run build
    cd ..
else
    echo "Node.js not found. Skipping frontend build."
    echo "Build the frontend locally and copy the dist/ folder."
fi

# ── Step 3: Copy .env ──
echo "[3/5] Setting up environment..."
if [ ! -f .env ]; then
    cp .env.production .env
    echo "Created .env from .env.production. Edit with production values if needed."
fi

# ── Step 4: Build and start containers ──
echo "[4/5] Building and starting Docker containers..."
sudo docker compose up -d --build

# ── Step 5: Verify ──
echo "[5/5] Verifying services..."
sleep 15
echo ""
echo "Service health checks:"
echo "  Spring Boot: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/health 2>/dev/null || echo 'starting...')"
echo "  Python ML:   $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8000/health 2>/dev/null || echo 'starting...')"
echo "  Node.js:     $(curl -s -o /dev/null -w '%{http_code}' http://localhost:5001/health 2>/dev/null || echo 'starting...')"
echo "  Nginx:       $(curl -s -o /dev/null -w '%{http_code}' http://localhost 2>/dev/null || echo 'starting...')"
echo ""

# Get public IP
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "<your-ec2-ip>")
echo "╔══════════════════════════════════════════╗"
echo "║  Deployment Complete!                    ║"
echo "╠══════════════════════════════════════════╣"
echo "║  Frontend: http://$PUBLIC_IP"
echo "║  API:      http://$PUBLIC_IP/api"
echo "║  Swagger:  http://$PUBLIC_IP/swagger-ui.html"
echo "╚══════════════════════════════════════════╝"
