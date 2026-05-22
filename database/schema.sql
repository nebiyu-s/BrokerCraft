-- BrokerCraft MySQL schema (XAMPP / port 3306)
-- Run in phpMyAdmin or: mysql -u root -p < database/schema.sql

CREATE DATABASE IF NOT EXISTS BrokerCraft
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE BrokerCraft;

-- ---------------------------------------------------------------------------
-- users
-- COMPANY is added as a new role alongside ADMIN, BROKER, CLIENT
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    role        ENUM('ADMIN', 'BROKER', 'CLIENT', 'COMPANY') NOT NULL,
    active      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- brokers (employee profiles)
-- ---------------------------------------------------------------------------
CREATE TABLE brokers (
    user_id     INT PRIMARY KEY,
    department  VARCHAR(100) NOT NULL DEFAULT 'General',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- clients (investor profiles)
-- ---------------------------------------------------------------------------
CREATE TABLE clients (
    user_id     INT PRIMARY KEY,
    email       VARCHAR(120) NOT NULL,
    balance     DECIMAL(15, 2) NOT NULL DEFAULT 100000.00,
    status      ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- assignments (one client -> one broker)
-- ---------------------------------------------------------------------------
CREATE TABLE assignments (
    client_id   INT PRIMARY KEY,
    broker_id   INT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(user_id) ON DELETE CASCADE,
    FOREIGN KEY (broker_id) REFERENCES brokers(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- stocks (market)
-- ---------------------------------------------------------------------------
CREATE TABLE stocks (
    symbol        VARCHAR(20)  PRIMARY KEY,
    company_name  VARCHAR(120) NOT NULL,
    price         DECIMAL(15, 2) NOT NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- portfolios (holdings per client)
-- ---------------------------------------------------------------------------
CREATE TABLE portfolios (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    client_id     INT NOT NULL,
    symbol        VARCHAR(20) NOT NULL,
    quantity      INT NOT NULL,
    average_price DECIMAL(15, 2) NOT NULL,
    UNIQUE KEY uk_client_symbol (client_id, symbol),
    FOREIGN KEY (client_id) REFERENCES clients(user_id) ON DELETE CASCADE,
    FOREIGN KEY (symbol) REFERENCES stocks(symbol) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- transactions (trade history)
-- ---------------------------------------------------------------------------
CREATE TABLE transactions (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    client_id   INT NOT NULL,
    broker_id   INT NULL,
    symbol      VARCHAR(20) NOT NULL,
    quantity    INT NOT NULL,
    price       DECIMAL(15, 2) NOT NULL,
    type        ENUM('BUY', 'SELL') NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(user_id) ON DELETE CASCADE,
    FOREIGN KEY (broker_id) REFERENCES brokers(user_id) ON DELETE SET NULL,
    FOREIGN KEY (symbol) REFERENCES stocks(symbol) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- companies
-- Stores the company profile after registration.
-- status: PENDING = waiting admin approval, APPROVED = can list IPO, REJECTED
-- ---------------------------------------------------------------------------
CREATE TABLE companies (
    user_id         INT PRIMARY KEY,
    email           VARCHAR(120) NOT NULL,
    description     TEXT,
    industry        VARCHAR(100) NOT NULL DEFAULT 'General',
    status          ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- ipo_listings
-- A company can submit one IPO listing per stock symbol.
-- status:
--   PENDING   = submitted, waiting admin approval
--   OPEN      = approved, clients can buy shares
--   CLOSED    = deadline passed or all shares sold
--   REJECTED  = admin rejected the IPO
-- ---------------------------------------------------------------------------
CREATE TABLE ipo_listings (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    company_id        INT NOT NULL,
    symbol            VARCHAR(20) NOT NULL UNIQUE,
    company_name      VARCHAR(120) NOT NULL,
    shares_offered    INT NOT NULL,
    shares_remaining  INT NOT NULL,
    price_per_share   DECIMAL(15, 2) NOT NULL,
    description       TEXT,
    deadline          DATE NOT NULL,
    status            ENUM('PENDING', 'OPEN', 'CLOSED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- seed: default admin
-- ---------------------------------------------------------------------------
INSERT INTO users (username, password, full_name, role, active)
VALUES ('admin', 'admin123', 'System Admin', 'ADMIN', 1);

-- ---------------------------------------------------------------------------
-- seed: Ethiopian market stocks (README)
-- ---------------------------------------------------------------------------
INSERT INTO stocks (symbol, company_name, price) VALUES
    ('ETHIO',   'Ethiopian Insurance', 250.00),
    ('DASHEN',  'Dashen Bank',         890.00),
    ('AWASH',   'Awash Bank',          620.00),
    ('HIBRET',  'Hibret Bank',         410.00),
    ('COMBANK', 'Commercial Bank',    1200.00);
