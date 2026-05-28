#!/bin/bash
export PATH=$PATH:/usr/local/bin
echo 'Waiting for old nodegroup stack deletion to complete...'
aws cloudformation wait stack-delete-complete --stack-name eksctl-insuranceiq-eks-nodegroup-standard-workers --region ap-south-2
echo 'Old stack deleted! Creating new nodegroup standard-workers with m7i-flex.large...'
eksctl create nodegroup \
  --cluster=insuranceiq-eks \
  --name=standard-workers \
  --node-type=m7i-flex.large \
  --nodes=2 \
  --nodes-min=2 \
  --nodes-max=3 \
  --managed \
  --region=ap-south-2
echo 'Nodegroup created successfully!'
aws eks update-kubeconfig --name insuranceiq-eks --region ap-south-2
echo 'Reinstalling NGINX Ingress Controller...'
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.service.type=LoadBalancer
echo 'Done!'
