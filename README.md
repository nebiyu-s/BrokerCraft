# BrokerCraft

## A Real-Time Multi-User Stock Brokerage & IPO Platform

Built with **Java 17 · JavaFX 21 · Java RMI · MySQL · Javalin (Web)**

---

## Project Overview

BrokerCraft is a distributed client-server stock brokerage and IPO management platform. It simulates a real-world financial ecosystem where:

- **Companies** raise capital by listing shares through an IPO process
- **Clients** invest by buying IPO shares and trading on the live market
- **Brokers** manage assigned clients and execute trades on their behalf
- **Admin** controls the entire platform through a web-based dashboard

The system demonstrates enterprise-level Java architecture including distributed computing, concurrent transaction processing, real-time market simulation, and role-based access control.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    BrokerCraft Server                    │
│                                                         │
│   RMI Registry (port 1099)   HTTP Server (port 7000)   │
│          ↓                          ↓                   │
│   BrokerCraftServiceImpl      AdminWebServer            │
│          ↓                          ↓                   │
│      DatabaseManager ←──────── IpoService               │
│          ↓                     AuthService              │
│        MySQL                   PriceSimulator           │
└─────────────────────────────────────────────────────────┘
         ↑                              ↑
   JavaFX Clients                  Browser (Admin)
  (Broker / Client /              http://localhost:7000/admin
   Company apps)
```

Two services run in the same server process:
- **RMI (port 1099)** — used by JavaFX desktop apps (Broker, Client, Company)
- **HTTP (port 7000)** — used by the Admin web dashboard in a browser

---

## User Roles

### Admin (Web Browser Dashboard)
- Accessed at `http://localhost:7000/admin`
- Login with admin credentials (default: `admin` / `admin123`)
- **Broker management:** create, edit, delete brokers, reset passwords
- **Client management:** approve/reject registrations, assign brokers, reassign, suspend, delete
- **Company management:** approve/reject company registrations, suspend companies
- **IPO management:** approve/reject IPO listings (approved IPOs go live on the market instantly)
- **Market control:** start/stop price simulation
- **Monitoring:** live stock prices, all transactions, active sessions

### Broker (JavaFX Desktop App)
- Login with role = Broker
- **Client book:** see all assigned clients, search by name
- **Client details:** view selected client's email, cash balance, total AUM
- **Client holdings:** portfolio table with Symbol, Shares, Avg Cost, Market Price, Value, P/L
- **Execute trades:** buy or sell stocks on behalf of any assigned client (requires confirmation checkbox)
- **Client history:** per-client transaction history with BUY/SELL filter
- **All activity:** combined transaction feed across all assigned clients

### Client (JavaFX Desktop App)
- Register via the app → wait for admin approval → login
- **Overview:** portfolio allocation, top movers, insight message
- **Market:** live prices with search, sort, change %, trend indicator
- **Portfolio:** holdings with Avg Cost, Market Price, Value, P/L (color-coded)
- **Trade:** buy/sell stocks at live prices with order preview
- **IPO Market:** buy shares from open IPO listings at fixed IPO price
- **History:** full transaction ledger with BUY/SELL filter and symbol search

### Company (JavaFX Desktop App)
- Register via the app → wait for admin approval → login
- **My IPOs:** view all submitted IPOs with status (PENDING/OPEN/CLOSED/REJECTED), shares sold %, capital raised
- **Submit IPO:** list shares for public offering (symbol, quantity, price per share, deadline, description)
- After admin approves → stock appears on the live market → clients can buy shares

---

## IPO Lifecycle

```
Company submits IPO
        ↓
   Status = PENDING
        ↓
Admin reviews in web dashboard
        ↓
   Admin approves
        ↓
   Status = OPEN
   Stock added to live market
        ↓
Clients buy IPO shares at fixed price
   Company receives capital
   Client receives shares in portfolio
        ↓
All shares sold OR deadline passed
        ↓
   Status = CLOSED
        ↓
Stock continues trading on secondary market
   Price fluctuates with simulation
   Clients can buy/sell freely
```

---

## Price Simulation

Prices update every **3 seconds** using **Geometric Brownian Motion** — the same mathematical model used in real financial markets.

```
new_price = current_price × exp(drift + volatility × gaussian_noise + momentum)
```

Parameters:
| Parameter | Value | Meaning |
|---|---|---|
| Volatility | 0.3% per tick | ~29% annual volatility (emerging market) |
| Drift | +0.005% per tick | Slight upward market bias |
| Momentum | 15% carry-over | Prices trend briefly, not pure noise |
| Floor | 20% of start price | Stock cannot collapse to zero |
| Ceiling | 400% of start price | Stock cannot go to infinity |

Simulation **starts automatically** when the server boots. Admin can stop/restart from the web dashboard.

---

## Database Schema

| Table | Purpose |
|---|---|
| `users` | Login credentials for all roles (ADMIN, BROKER, CLIENT, COMPANY) |
| `brokers` | Broker department info |
| `clients` | Client email, balance, approval status |
| `assignments` | Maps each client to exactly one broker |
| `stocks` | Live market stock data (updated by price simulator) |
| `portfolios` | Client holdings (symbol, quantity, average cost) |
| `transactions` | Full trade history with timestamps |
| `companies` | Company profile, industry, approval status |
| `ipo_listings` | IPO details: symbol, shares, price, deadline, status |

---

## Setup & Running

### Requirements
- Java 17+
- XAMPP (MySQL on port 3306)
- Gradle (included via wrapper)

### 1. Database Setup

Start XAMPP → start MySQL → open phpMyAdmin → run `database/schema.sql`

Or run the additional tables if upgrading:
```sql
ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN','BROKER','CLIENT','COMPANY') NOT NULL;

CREATE TABLE IF NOT EXISTS companies (
    user_id INT PRIMARY KEY, email VARCHAR(120) NOT NULL,
    description TEXT, industry VARCHAR(100) NOT NULL DEFAULT 'General',
    status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ipo_listings (
    id INT AUTO_INCREMENT PRIMARY KEY, company_id INT NOT NULL,
    symbol VARCHAR(20) NOT NULL UNIQUE, company_name VARCHAR(120) NOT NULL,
    shares_offered INT NOT NULL, shares_remaining INT NOT NULL,
    price_per_share DECIMAL(15,2) NOT NULL, description TEXT,
    deadline DATE NOT NULL,
    status ENUM('PENDING','OPEN','CLOSED','REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;
```

### 2. Database Credentials

Copy `src/main/resources/db.properties.example` to `src/main/resources/db.properties`:
```properties
jdbc.url=jdbc:mysql://localhost:3306/BrokerCraft?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
jdbc.user=root
jdbc.password=
```

### 3. Start the Server
```bash
./gradlew runServer
```
Expected output:
```
Connected to MySQL database: BrokerCraft
Price simulation started automatically.
RMI registry started on port 1099
Admin web dashboard → http://localhost:7000/admin
BrokerCraft server is running.
```

### 4. Start the JavaFX Client
```bash
./gradlew run
```

### 5. Admin Dashboard
Open browser: `http://localhost:7000/admin`
Login: `admin` / `admin123`

---

## Default Credentials

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Broker | Created by admin | Set by admin |
| Client | Self-registered | Set during registration |
| Company | Self-registered | Set during registration |

---

## Seed Data

5 Ethiopian market stocks pre-loaded:

| Symbol | Company | Starting Price |
|---|---|---|
| ETHIO | Ethiopian Insurance | 250.00 ETB |
| DASHEN | Dashen Bank | 890.00 ETB |
| AWASH | Awash Bank | 620.00 ETB |
| HIBRET | Hibret Bank | 410.00 ETB |
| COMBANK | Commercial Bank | 1,200.00 ETB |

New clients start with **100,000 ETB** balance.

---

## Key Technical Concepts Demonstrated

| Concept | Implementation |
|---|---|
| Distributed Systems | Java RMI for client-server communication |
| Concurrent Programming | ReentrantLock per client for thread-safe trades |
| Real-Time Systems | RMI callbacks push price updates to all connected clients |
| Event-Driven Programming | JavaFX ObservableList + listeners for live UI updates |
| Financial Modeling | Geometric Brownian Motion for realistic price simulation |
| Role-Based Access Control | 4 distinct roles with separate dashboards and permissions |
| Web + Desktop Hybrid | Admin uses browser (Javalin HTTP), others use JavaFX |
| Database Integration | MySQL with parameterized queries, transactions, batch operations |
| File Logging | Every trade appended to `logs/transactions.log` |
| Object Serialization | All model classes implement Serializable for RMI transport |

---

## Package Structure

```
src/main/java/brokercraft/
├── controllers/        JavaFX controllers (Login, Register, Broker, Client, Company)
├── database/           DatabaseManager, DatabaseConnection, Db utility
├── main/               Main, ServerMain, SceneRouter, SessionContext
├── model/              User, Stock, Transaction, Portfolio, IpoListing, CompanyProfile...
├── network/            RMIClient, ClientPriceListener
├── rmi/                BrokerCraftService (interface), BrokerCraftServiceImpl, RMIServer
├── service/            AuthService, TransactionService, IpoService
├── simulation/         PriceSimulator (Geometric Brownian Motion)
├── synchronization/    TransactionLockManager (per-client ReentrantLock)
├── utils/              StyleManager, ChartHelper, TransactionLogger, UiClock
└── web/                AdminWebServer, AdminHtmlPage (Javalin HTTP + HTML dashboard)
```

---

## Transaction Flow (Buy Example)

```
Client clicks BUY (50 AWASH shares)
        ↓
JavaFX → RMI call → BrokerCraftServiceImpl.executeTrade()
        ↓
TransactionService.executeTrade()
        ↓
TransactionLockManager.getLock(clientId).lock()   ← thread safety
        ↓
Validate: balance >= 50 × current_price?
        ↓
Deduct balance from clients table
        ↓
Add 50 AWASH to portfolios table (update avg cost)
        ↓
Insert row into transactions table
        ↓
TransactionLogger.log() → append to logs/transactions.log
        ↓
lock.unlock()
        ↓
Return "SUCCESS" to client
        ↓
JavaFX refreshes portfolio, balance, history
```
