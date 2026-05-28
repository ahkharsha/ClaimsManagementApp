#!/bin/bash
# Setup K3s on the Private Node (Ubuntu 22.04)

# Install K3s (lightweight Kubernetes) without Traefik since we will use Nginx on Bastion for external access,
# but actually we CAN keep Traefik for internal ingress routing.
curl -sfL https://get.k3s.io | sh -

# Wait for K3s to be ready
sleep 15
sudo k3s kubectl get nodes

# Make kubeconfig readable by ubuntu user
sudo mkdir -p /home/ubuntu/.kube
sudo cp /etc/rancher/k3s/k3s.yaml /home/ubuntu/.kube/config
sudo chown ubuntu:ubuntu /home/ubuntu/.kube/config
echo "export KUBECONFIG=/home/ubuntu/.kube/config" >> /home/ubuntu/.bashrc

# Install Helm
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh

# Install Prometheus & Grafana using Helm
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace insuranceiq \
  --create-namespace \
  -f /home/ubuntu/k8s/monitoring/values.yaml
