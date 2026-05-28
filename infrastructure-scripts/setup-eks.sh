#!/bin/bash
curl --silent --location "https://github.com/eksctl-io/eksctl/releases/latest/download/eksctl_Linux_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
eksctl version

echo 'Creating EKS Cluster insuranceiq-eks...'
eksctl create cluster \
  --name insuranceiq-eks \
  --region ap-south-2 \
  --nodegroup-name standard-workers \
  --node-type m7i-flex.large \
  --nodes 2 \
  --nodes-min 2 \
  --nodes-max 3 \
  --managed

echo 'EKS Cluster created!'

aws eks update-kubeconfig --name insuranceiq-eks --region ap-south-2

echo 'Installing NGINX Ingress Controller...'
/usr/local/bin/helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
/usr/local/bin/helm repo update
/usr/local/bin/helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.service.type=LoadBalancer

echo 'Done!'
