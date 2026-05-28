$ErrorActionPreference = "Stop"

Write-Host "Checking for eksctl..."
if (-not (Get-Command eksctl -ErrorAction SilentlyContinue)) {
    Write-Host "eksctl is not installed. Please install it first (e.g. via 'choco install eksctl')."
    exit 1
}

$CLUSTER_NAME = "insuranceiq-eks"
$REGION = "ap-south-2"
$NODE_TYPE = "m7i-flex.large"
$NODE_COUNT = 2

Write-Host "Creating EKS Cluster '$CLUSTER_NAME' in '$REGION' with $NODE_COUNT $NODE_TYPE nodes..."
Write-Host "This will take approximately 15-20 minutes. Do not close the window."

# Create cluster
eksctl create cluster `
    --name $CLUSTER_NAME `
    --region $REGION `
    --nodegroup-name standard-workers `
    --node-type $NODE_TYPE `
    --nodes $NODE_COUNT `
    --nodes-min $NODE_COUNT `
    --nodes-max 3 `
    --managed

Write-Host "EKS Cluster provisioned successfully!"
Write-Host "Configuring local kubectl context..."
aws eks update-kubeconfig --name $CLUSTER_NAME --region $REGION

Write-Host "Installing NGINX Ingress Controller on EKS..."
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx `
  --namespace ingress-nginx --create-namespace `
  --set controller.service.type=LoadBalancer

Write-Host "Done! Infrastructure is ready."
