$ErrorActionPreference = "Stop"
$env:PATH += ";C:\Users\ahkha\AppData\Roaming\Python\Python312\Scripts"

Write-Host "Creating VPC..."
$vpcId = (aws ec2 create-vpc --cidr-block 10.0.0.0/16 --query "Vpc.VpcId" --output text).Trim()
aws ec2 create-tags --resources $vpcId --tags Key=Name,Value=insuranceiq-vpc

Write-Host "Creating Subnets..."
$pubSubnetId = (aws ec2 create-subnet --vpc-id $vpcId --cidr-block 10.0.1.0/24 --availability-zone ap-south-2a --query "Subnet.SubnetId" --output text).Trim()
aws ec2 create-tags --resources $pubSubnetId --tags Key=Name,Value=insuranceiq-public-subnet

$privSubnetId = (aws ec2 create-subnet --vpc-id $vpcId --cidr-block 10.0.2.0/24 --availability-zone ap-south-2b --query "Subnet.SubnetId" --output text).Trim()
aws ec2 create-tags --resources $privSubnetId --tags Key=Name,Value=insuranceiq-private-subnet

Write-Host "Creating Internet Gateway..."
$igwId = (aws ec2 create-internet-gateway --query "InternetGateway.InternetGatewayId" --output text).Trim()
aws ec2 attach-internet-gateway --vpc-id $vpcId --internet-gateway-id $igwId

Write-Host "Configuring Route Tables..."
$pubRouteTableId = (aws ec2 create-route-table --vpc-id $vpcId --query "RouteTable.RouteTableId" --output text).Trim()
aws ec2 create-route --route-table-id $pubRouteTableId --destination-cidr-block 0.0.0.0/0 --gateway-id $igwId | Out-Null
aws ec2 associate-route-table --subnet-id $pubSubnetId --route-table-id $pubRouteTableId | Out-Null

Write-Host "Creating Security Groups..."
$bastionSgId = (aws ec2 create-security-group --group-name "bastion-sg" --description "SG for Jenkins/Nginx bastion" --vpc-id $vpcId --query "GroupId" --output text).Trim()
aws ec2 authorize-security-group-ingress --group-id $bastionSgId --protocol tcp --port 22 --cidr 0.0.0.0/0 | Out-Null
aws ec2 authorize-security-group-ingress --group-id $bastionSgId --protocol tcp --port 80 --cidr 0.0.0.0/0 | Out-Null
aws ec2 authorize-security-group-ingress --group-id $bastionSgId --protocol tcp --port 8082 --cidr 0.0.0.0/0 | Out-Null

$k3sSgId = (aws ec2 create-security-group --group-name "k3s-sg" --description "SG for private K3s cluster" --vpc-id $vpcId --query "GroupId" --output text).Trim()
aws ec2 authorize-security-group-ingress --group-id $k3sSgId --protocol tcp --port 22 --source-group $bastionSgId | Out-Null
aws ec2 authorize-security-group-ingress --group-id $k3sSgId --protocol tcp --port 80 --source-group $bastionSgId | Out-Null
aws ec2 authorize-security-group-ingress --group-id $k3sSgId --protocol tcp --port 6443 --source-group $bastionSgId | Out-Null
# Allow NodePorts from bastion
aws ec2 authorize-security-group-ingress --group-id $k3sSgId --protocol tcp --port 30000-32767 --source-group $bastionSgId | Out-Null
# Allow pods to talk to each other
aws ec2 authorize-security-group-ingress --group-id $k3sSgId --protocol -1 --source-group $k3sSgId | Out-Null

Write-Host "Creating Key Pair..."
if (Test-Path -Path "insuranceiq-key.pem") {
    Remove-Item "insuranceiq-key.pem"
}
aws ec2 create-key-pair --key-name insuranceiq-key --query "KeyMaterial" --output text > insuranceiq-key.pem
# No chmod needed on Windows

Write-Host "Launching EC2 Instances..."
# Ubuntu 22.04 LTS AMI for ap-south-2
$amiId = "ami-0eab39170eb2844c5"

$bastionId = (aws ec2 run-instances --image-id $amiId --count 1 --instance-type t3.small --key-name insuranceiq-key --security-group-ids $bastionSgId --subnet-id $pubSubnetId --associate-public-ip-address --query "Instances[0].InstanceId" --output text).Trim()
aws ec2 create-tags --resources $bastionId --tags Key=Name,Value=insuranceiq-bastion

$k3sId = (aws ec2 run-instances --image-id $amiId --count 1 --instance-type t3.medium --key-name insuranceiq-key --security-group-ids $k3sSgId --subnet-id $privSubnetId --query "Instances[0].InstanceId" --output text).Trim()
aws ec2 create-tags --resources $k3sId --tags Key=Name,Value=insuranceiq-k3s

Write-Host "Waiting for instances to be running..."
aws ec2 wait instance-running --instance-ids $bastionId $k3sId

$bastionIp = (aws ec2 describe-instances --instance-ids $bastionId --query "Reservations[0].Instances[0].PublicIpAddress" --output text).Trim()
$k3sIp = (aws ec2 describe-instances --instance-ids $k3sId --query "Reservations[0].Instances[0].PrivateIpAddress" --output text).Trim()

Write-Host "========================================="
Write-Host "VPC ID: $vpcId"
Write-Host "Bastion Public IP: $bastionIp"
Write-Host "K3s Private IP: $k3sIp"
Write-Host "========================================="

Write-Host "Creating ECR Repos..."
$repos = @("insuranceiq-frontend", "insuranceiq-server", "insuranceiq-ml", "insuranceiq-notification")
foreach ($repo in $repos) {
    aws ecr create-repository --repository-name $repo | Out-Null
    Write-Host "Created ECR repo: $repo"
}

Write-Host "Infrastructure provisioning complete!"
