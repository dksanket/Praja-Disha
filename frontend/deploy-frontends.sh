#!/bin/bash
set -e

echo "🚀 Starting Deployment Pipeline for Praja-Disha..."

# 1. Build Citizen App
echo "🏗️ Building Citizen UI Application..."
cd Citizen-App
npm install
npm run build -- --configuration=production
cd ..

# 2. Build Admin Portal
echo "🏗️ Building Org Admin Dashboard Application..."
cd Org-Admin-Dashboard
npm install
npm run build -- --configuration=production
cd ..

# 3. Deploy everything to Firebase
echo "🌍 Deploying targets to Firebase Hosting..."
firebase deploy --only hosting

echo "✅ Deployment complete successfully!"