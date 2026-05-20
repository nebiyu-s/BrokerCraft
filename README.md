# BrokerCraft

## A Real-Time Multi-User Stock Brokerage Management System Using JavaFX, RMI, Multithreading, and Database Integration

---

# Project Overview

BrokerCraft is a distributed client-server stock brokerage management platform developed using JavaFX and advanced Java programming concepts. The system simulates the workflow of a real-world brokerage company where Clients can trade stocks, Brokers can manage assigned clients and execute trades, and the Admin controls and monitors the entire platform.

The application integrates:

* JavaFX GUI development
* Event-driven programming
* Java RMI communication
* TCP Socket programming
* Multithreading and synchronization
* Database CRUD operations
* File handling and logging
* Object serialization
* Real-time stock market simulation

The system demonstrates how enterprise financial platforms handle concurrent users, transactions, market updates, and role-based access control.

---

#  Real-World Inspiration

BrokerCraft is inspired by modern brokerage systems such as:

* Robinhood
* Fidelity
* E*TRADE
* Traditional brokerage firms

The project simulates both:

* Self-service investing (Client trades independently)
* Broker-assisted investing (Broker executes trades for clients)

This creates a more realistic financial ecosystem.

---

# main Objectives

The project aims to:

* Simulate a real brokerage workflow
* Demonstrate distributed systems using RMI
* Implement concurrent transaction processing
* Provide real-time market updates
* Demonstrate synchronization and thread safety
* Implement role-based dashboards
* Showcase enterprise-level Java architecture
* Simulate stock trading and portfolio management

---

# User Roles

# 1. Admin

The Admin represents the owner or manager of the brokerage company.

Admin responsibilities:

* Create Broker accounts
* Approve Client registrations
* Assign Clients to Brokers
* Monitor all transactions
* Start and stop price simulation
* View active users
* Manage system operations
* Monitor overall trading activities

---

# 2. Broker

The Broker represents an employee or financial advisor.

Broker responsibilities:

* Manage assigned clients
* Monitor client portfolios
* Execute trades for clients
* View live market prices
* Monitor transaction activity

Each Broker can only access their assigned clients.

---

# 3. Client

The Client represents a customer or investor.

Client responsibilities:

* Register on the platform
* Buy and sell shares
* View portfolio
* Monitor stock prices
* View transaction history
* Manage investments

---

# 🔐 Authentication & Registration Workflow

# Broker Registration Flow

Only Admin can create Broker accounts.

Workflow:

```text
Admin Login
    ↓
Manage Brokers
    ↓
Create Broker Account
    ↓
Broker Becomes Active
```

This is realistic because Brokers are employees of the company.

---

# Client Registration Flow

Clients register by themselves but require Admin approval.

Workflow:

```text
Client Opens Application
    ↓
Clicks Register
    ↓
Fills Registration Form
    ↓
Status = Pending
    ↓
Admin Reviews Request
    ↓
Admin Approves Client
    ↓
Admin Assigns Broker
    ↓
Client Becomes Active
```

---

# 🔗 Broker Assignment Workflow

Every Client is assigned to exactly one Broker.

Relationship:

```text
One Broker → Many Clients
One Client → One Broker
```

Workflow:

```text
Client Registers
    ↓
Admin Reviews Request
    ↓
Admin Selects Broker
    ↓
Assignment Saved in Database
    ↓
Broker Can Now Access Client
```

This makes the system realistic and demonstrates access control.

---

# 🖥️ Full System Workflow

# Step 1 — Server Startup

The Server application starts first.

Server responsibilities:

* Start RMI Registry
* Connect to database
* Initialize stock market data
* Start background services
* Start price simulation thread
* Listen for client connections

Workflow:

```text
Start Server
    ↓
Initialize Database
    ↓
Start RMI Registry
    ↓
Load Stocks
    ↓
Start Price Simulation Thread
    ↓
Wait For Client Connections
```

---

# Step 2 — Admin Login

Admin logs into the platform.

Admin Dashboard loads:

* System monitor
* User management
* Broker management
* Pending registrations
* Market controls
* Live transactions

Admin can:

* Create Brokers
* Approve Clients
* Assign Brokers
* Start/stop simulation
* Monitor everything

---

# Step 3 — Broker Login

Broker logs into the system.

Broker Dashboard loads:

* Assigned client list
* Portfolio management
* Live prices
* Trade execution panel
* Transaction monitoring

Broker only sees assigned clients.

---

# Step 4 — Client Login

Client logs into the platform.

Client Dashboard loads:

* Live market prices
* Portfolio table
* Balance display
* Buy/Sell controls
* Transaction history
* Assigned broker information

---

# Stock Price Simulation Workflow

The system includes a real-time market simulator.

A background thread continuously updates stock prices.

Example stocks:

* ETHIO
* DASHEN
* AWASH
* HIBRET
* COMBANK

Workflow:

```text
Price Thread Starts
    ↓
Every 3 Seconds:
    ↓
Generate Random Price Change
    ↓
Update Shared Stock Data
    ↓
Notify Connected Clients
    ↓
Refresh JavaFX Tables Automatically
```

---

# Multithreading Architecture

BrokerCraft heavily uses Threads.

Main threads:

| Thread                     | Purpose                  |
| -------------------------- | ------------------------ |
| Price Simulation Thread    | Updates market prices    |
| Client Connection Threads  | Handle multiple users    |
| Transaction Threads        | Process trades           |
| Background Database Thread | Save data asynchronously |
| UI Thread (JavaFX Thread)  | GUI rendering            |

---

# Synchronization Workflow

Synchronization prevents race conditions.

Example problem:

Two users try to buy shares at the same time.

Without synchronization:

* balance corruption
* inconsistent portfolios
* invalid transactions

Solution:

```text
Transaction Request
    ↓
Acquire Lock
    ↓
Validate Balance
    ↓
Update Portfolio
    ↓
Update Database
    ↓
Release Lock
```

This ensures thread-safe operations.

---

# Complete Buy Transaction Workflow

# Example Scenario

Client Abel buys 100 ETHIO shares.

---

# Step-by-Step Technical Flow

## 1. GUI Interaction

Abel:

* selects ETHIO
* enters quantity
* clicks Buy

JavaFX EventHandler captures the action.

---

## 2. Request Sent to Server

Client application sends BuyRequest object using RMI.

Example:

```text
Client → RMI → Server
```

---

## 3. Server Validation

Server checks:

* client authentication
* sufficient balance
* current stock price
* market availability

---

## 4. Synchronization Lock

Server locks the client account to avoid concurrent modification.

---

## 5. Transaction Processing

Server:

* deducts balance
* adds shares to portfolio
* creates transaction object

---

## 6. Database Update

Database saves:

* updated balance
* updated portfolio
* transaction history

---

## 7. File Logging

Transaction is written to log file.

Example:

```text
[12:45 PM] Abel bought 100 ETHIO shares at 250 ETB
```

---

## 8. Response Returned

Server sends success response.

---

## 9. GUI Updates Automatically

Client dashboard refreshes:

* new balance
* updated portfolio
* transaction history

---

# Real-Time Update Workflow

After several seconds:

ETHIO price changes from:

```text
250 ETB → 265 ETB
```

The system:

```text
Price Thread Updates Price
    ↓
Server Broadcasts Update
    ↓
Broker & Clients Receive Notification
    ↓
JavaFX Tables Refresh Automatically
```

No manual refresh required.

---

# Client-Server Communication

# Communication Model

```text
Client Applications
        ↓
    Java RMI
        ↓
BrokerCraft Server
        ↓
Database + File System
```

The server acts as the central authority.

All clients communicate only with the server.

---

# JavaFX GUI Design

# Login Screen

Components:

* Username field
* Password field
* Role selection ComboBox
* Login button
* Register button
* Error message label

Design style:

* Modern financial theme
* Dark mode dashboard colors
* Professional gradients
* Rounded controls
* Smooth hover effects

---

# Client Dashboard

Sections:

```text
Top Navigation Bar
    ↓
Live Market Prices Table
    ↓
Portfolio Table
    ↓
Transaction History
    ↓
Buy/Sell Panel
```

Features:

* Real-time updates
* TableView integration
* Dynamic labels
* ObservableList binding

---

# Broker Dashboard

Sections:

```text
Assigned Clients Panel
    ↓
Client Portfolio Viewer
    ↓
Trade Execution Panel
    ↓
Live Transactions Feed
```

---

# Admin Dashboard

Sections:

```text
System Monitor
    ↓
Pending Registrations
    ↓
Broker Management
    ↓
Live Market Controls
    ↓
Analytics & Statistics
```

---

# Recommended Package Structure

```text
src/
├── admin/
├── broker/
├── client/
├── controllers/
├── database/
├── gui/
├── model/
├── network/
├── rmi/
├── service/
├── simulation/
├── synchronization/
├── threads/
├── utils/
└── main/
```

---

# 🧩 Core Classes

| Class             | Responsibility            |
| ----------------- | ------------------------- |
| User              | Base user information     |
| Client            | Client data and portfolio |
| Broker            | Broker operations         |
| Admin             | Administrative operations |
| Stock             | Market stock information  |
| Portfolio         | User holdings             |
| Transaction       | Trade information         |
| PriceSimulator    | Real-time price updates   |
| DatabaseManager   | CRUD operations           |
| RMIServer         | Remote communication      |
| TransactionLogger | File logging              |

---

# Database Design

Main tables:

| Table        | Purpose                  |
| ------------ | ------------------------ |
| users        | Stores login information |
| clients      | Client details           |
| brokers      | Broker details           |
| stocks       | Market stock data        |
| portfolios   | Client holdings          |
| transactions | Trade history            |
| assignments  | Broker-client mapping    |

---

# File Handling

The project uses file handling for:

* transaction logs
* backups
* serialized objects
* reports

---

# Collections Framework Usage

Collections used:

| Collection | Usage             |
| ---------- | ----------------- |
| ArrayList  | Stock lists       |
| HashMap    | User sessions     |
| Queue      | Transaction queue |
| Set        | Active users      |

---

# RMI Usage

RMI is used for:

* login requests
* buy/sell requests
* market updates
* portfolio retrieval
* transaction synchronization

---

# Testing Areas

The system should be tested for:

* concurrent transactions
* synchronization correctness
* database consistency
* RMI communication
* GUI responsiveness
* role permissions
* market simulation accuracy

---

# Educational Simplifications

Compared to real brokerage systems:

* prices are simulated
* no real exchange connection exists
* simplified security model
* no advanced matching engine
* no regulatory compliance

These simplifications keep the project manageable while still demonstrating advanced programming concepts.

---

# suggested Development Order

# Phase 1

Create database and models.

---

# Phase 2

Build server and RMI architecture.

---

# Phase 3

Implement authentication and registration.

---

# Phase 4

Build JavaFX dashboards.

---

# Phase 5

Implement stock simulation threads.

---

# Phase 6

Implement transaction processing.

---

# Phase 7

Add synchronization and validation.

---

# Phase 8

Add logging, reports, and analytics.

---

# Phase 9

Optimize UI and performance.

---

#  Concepts Demonstrated

BrokerCraft demonstrates:

* Object-Oriented Programming
* Distributed Systems
* Concurrent Programming
* Event-Driven Programming
* Client-Server Architecture
* Database Integration
* Network Communication
* Real-Time Systems
* Role-Based Access Control

---

# Final Summary

BrokerCraft is a comprehensive distributed brokerage simulation platform that integrates JavaFX GUI development, multithreading, synchronization, RMI communication, database persistence, and real-time market simulation into one enterprise-style application.

The project demonstrates how real brokerage systems coordinate multiple users, concurrent transactions, live market updates, and secure role-based operations while maintaining responsiveness, consistency, and scalability.
