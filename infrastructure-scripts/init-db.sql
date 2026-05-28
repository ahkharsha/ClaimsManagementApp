-- InsuranceIQ — Database Initialization
-- This script runs automatically when the MySQL container starts for the first time.

CREATE DATABASE IF NOT EXISTS insuranceiq_db;
CREATE DATABASE IF NOT EXISTS insuranceiq_notifications;

-- Grant permissions
GRANT ALL PRIVILEGES ON insuranceiq_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON insuranceiq_notifications.* TO 'root'@'%';
FLUSH PRIVILEGES;
