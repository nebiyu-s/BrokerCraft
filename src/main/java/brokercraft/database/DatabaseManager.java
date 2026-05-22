package brokercraft.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import brokercraft.model.BrokerProfile;
import brokercraft.model.ClientProfile;
import brokercraft.model.CompanyProfile;
import brokercraft.model.IpoListing;
import brokercraft.model.Portfolio;
import brokercraft.model.PortfolioItem;
import brokercraft.model.RegistrationStatus;
import brokercraft.model.Stock;
import brokercraft.model.Transaction;
import brokercraft.model.TransactionType;
import brokercraft.model.User;
import brokercraft.model.UserRole;

public class DatabaseManager {
    private static DatabaseManager instance;
    private volatile boolean initialized;

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        try {
            DatabaseConnection.testConnection();
            ensureSeedData();
            initialized = true;
            System.out.println("Connected to MySQL database: BrokerCraft");
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Database initialization failed. Run database/schema.sql in XAMPP MySQL first.\n"
                            + "Details: " + e.getMessage(), e);
        }
    }

    private void ensureSeedData() throws SQLException {
        if (findUserByUsername("admin").isEmpty()) {
            User admin = new User(0, "admin", "admin123", "System Admin", UserRole.ADMIN, true);
            saveUser(admin);
        }
        if (getAllStocks().isEmpty()) {
            seedStocks();
        }
    }

    private void seedStocks() throws SQLException {
        saveStock(new Stock("ETHIO", "Ethiopian Insurance", 250));
        saveStock(new Stock("DASHEN", "Dashen Bank", 890));
        saveStock(new Stock("AWASH", "Awash Bank", 620));
        saveStock(new Stock("HIBRET", "Hibret Bank", 410));
        saveStock(new Stock("COMBANK", "Commercial Bank", 1200));
    }

    public User saveUser(User user) throws SQLException {
        if (user.getId() == 0) {
            String sql = "INSERT INTO users (username, password, full_name, role, active) VALUES (?,?,?,?,?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPassword());
                ps.setString(3, user.getFullName());
                ps.setString(4, user.getRole().name());
                ps.setBoolean(5, user.isActive());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setId(keys.getInt(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE users SET username=?, password=?, full_name=?, role=?, active=? WHERE id=?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPassword());
                ps.setString(3, user.getFullName());
                ps.setString(4, user.getRole().name());
                ps.setBoolean(5, user.isActive());
                ps.setInt(6, user.getId());
                ps.executeUpdate();
            }
        }
        return user;
    }

    public Optional<User> findUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findUserById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
            }
        }
    }

    public List<User> findUsersByRole(UserRole role) throws SQLException {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name";
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUser(rs));
                }
            }
        }
        return list;
    }

    public ClientProfile saveClientProfile(ClientProfile profile) throws SQLException {
        String sql = """
                INSERT INTO clients (user_id, email, balance, status)
                VALUES (?,?,?,?)
                ON DUPLICATE KEY UPDATE email=VALUES(email), balance=VALUES(balance), status=VALUES(status)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, profile.getUserId());
            ps.setString(2, profile.getEmail());
            ps.setDouble(3, profile.getBalance());
            ps.setString(4, profile.getStatus().name());
            ps.executeUpdate();
        }

        if (profile.getAssignedBrokerId() != null) {
            saveAssignment(profile.getUserId(), profile.getAssignedBrokerId());
        }
        return profile;
    }

    private void saveAssignment(int clientId, int brokerId) throws SQLException {
        String sql = """
                INSERT INTO assignments (client_id, broker_id) VALUES (?,?)
                ON DUPLICATE KEY UPDATE broker_id=VALUES(broker_id), assigned_at=CURRENT_TIMESTAMP
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            ps.setInt(2, brokerId);
            ps.executeUpdate();
        }
    }

    public Optional<ClientProfile> findClientProfile(int userId) throws SQLException {
        String sql = """
                SELECT c.user_id, c.email, c.balance, c.status, a.broker_id
                FROM clients c
                LEFT JOIN assignments a ON a.client_id = c.user_id
                WHERE c.user_id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapClientProfile(rs)) : Optional.empty();
            }
        }
    }

    public List<ClientProfile> findClientsByBroker(int brokerId) throws SQLException {
        String sql = """
                SELECT c.user_id, c.email, c.balance, c.status, a.broker_id
                FROM clients c
                INNER JOIN assignments a ON a.client_id = c.user_id
                WHERE a.broker_id = ?
                """;
        List<ClientProfile> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, brokerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapClientProfile(rs));
                }
            }
        }
        return list;
    }

    public List<ClientProfile> findPendingClients() throws SQLException {
        String sql = """
                SELECT c.user_id, c.email, c.balance, c.status, a.broker_id
                FROM clients c
                LEFT JOIN assignments a ON a.client_id = c.user_id
                WHERE c.status = 'PENDING'
                """;
        List<ClientProfile> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapClientProfile(rs));
                }
            }
        }
        return list;
    }

    public BrokerProfile saveBrokerProfile(BrokerProfile profile) throws SQLException {
        String sql = """
                INSERT INTO brokers (user_id, department) VALUES (?,?)
                ON DUPLICATE KEY UPDATE department=VALUES(department)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, profile.getUserId());
            ps.setString(2, profile.getDepartment());
            ps.executeUpdate();
        }
        return profile;
    }

    public Optional<BrokerProfile> findBrokerProfile(int userId) throws SQLException {
        String sql = "SELECT * FROM brokers WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new BrokerProfile(rs.getInt("user_id"), rs.getString("department")));
            }
        }
    }

    public Portfolio getPortfolio(int clientId) throws SQLException {
        Portfolio portfolio = new Portfolio(clientId);
        String sql = "SELECT symbol, quantity, average_price FROM portfolios WHERE client_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    portfolio.getHoldings().add(new PortfolioItem(
                            rs.getString("symbol"),
                            rs.getInt("quantity"),
                            rs.getDouble("average_price")));
                }
            }
        }
        return portfolio;
    }

    public void savePortfolio(Portfolio portfolio) throws SQLException {
        int clientId = portfolio.getClientId();
        String deleteSql = "DELETE FROM portfolios WHERE client_id = ?";
        String insertSql = "INSERT INTO portfolios (client_id, symbol, quantity, average_price) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delete = conn.prepareStatement(deleteSql);
                 PreparedStatement insert = conn.prepareStatement(insertSql)) {
                delete.setInt(1, clientId);
                delete.executeUpdate();
                for (PortfolioItem item : portfolio.getHoldings()) {
                    insert.setInt(1, clientId);
                    insert.setString(2, item.getSymbol().toUpperCase());
                    insert.setInt(3, item.getQuantity());
                    insert.setDouble(4, item.getAveragePrice());
                    insert.addBatch();
                }
                insert.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void saveStock(Stock stock) throws SQLException {
        String sql = """
                INSERT INTO stocks (symbol, company_name, price) VALUES (?,?,?)
                ON DUPLICATE KEY UPDATE company_name=VALUES(company_name), price=VALUES(price)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stock.getSymbol().toUpperCase());
            ps.setString(2, stock.getCompanyName());
            ps.setDouble(3, stock.getPrice());
            ps.executeUpdate();
        }
    }

    public Optional<Stock> findStock(String symbol) throws SQLException {
        String sql = "SELECT * FROM stocks WHERE symbol = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapStock(rs)) : Optional.empty();
            }
        }
    }

    public List<Stock> getAllStocks() throws SQLException {
        String sql = "SELECT * FROM stocks ORDER BY symbol";
        List<Stock> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapStock(rs));
            }
        }
        return list;
    }

    public synchronized Transaction saveTransaction(Transaction tx) throws SQLException {
        String sql = """
                INSERT INTO transactions (client_id, broker_id, symbol, quantity, price, type)
                VALUES (?,?,?,?,?,?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tx.getClientId());
            if (tx.getBrokerId() != null) {
                ps.setInt(2, tx.getBrokerId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, tx.getSymbol().toUpperCase());
            ps.setInt(4, tx.getQuantity());
            ps.setDouble(5, tx.getPrice());
            ps.setString(6, tx.getType().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    tx.setId(keys.getInt(1));
                }
            }
        }
        return tx;
    }

    public List<Transaction> getTransactionsForClient(int clientId) throws SQLException {
        return queryTransactions("WHERE t.client_id = ? ORDER BY t.created_at DESC", clientId);
    }

    public List<Transaction> getAllTransactions() throws SQLException {
        return queryTransactions("ORDER BY t.created_at DESC");
    }

    public List<Transaction> getTransactionsForBrokerClients(int brokerId) throws SQLException {
        String sql = """
                SELECT t.*, u.full_name AS client_name
                FROM transactions t
                INNER JOIN users u ON u.id = t.client_id
                INNER JOIN assignments a ON a.client_id = t.client_id
                WHERE a.broker_id = ?
                ORDER BY t.created_at DESC
                """;
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, brokerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        }
        return list;
    }

    private List<Transaction> queryTransactions(String suffix, Object... params) throws SQLException {
        String sql = """
                SELECT t.*, u.full_name AS client_name
                FROM transactions t
                INNER JOIN users u ON u.id = t.client_id
                """ + suffix;
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        }
        return list;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name"),
                UserRole.valueOf(rs.getString("role")),
                rs.getBoolean("active"));
    }

    private ClientProfile mapClientProfile(ResultSet rs) throws SQLException {
        Object brokerObj = rs.getObject("broker_id");
        Integer brokerId = brokerObj == null ? null : ((Number) brokerObj).intValue();
        return new ClientProfile(
                rs.getInt("user_id"),
                rs.getString("email"),
                rs.getDouble("balance"),
                RegistrationStatus.valueOf(rs.getString("status")),
                brokerId);
    }

    private Stock mapStock(ResultSet rs) throws SQLException {
        return new Stock(
                rs.getString("symbol"),
                rs.getString("company_name"),
                rs.getDouble("price"));
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction(
                rs.getInt("id"),
                rs.getInt("client_id"),
                rs.getString("client_name"),
                rs.getObject("broker_id") != null ? rs.getInt("broker_id") : null,
                rs.getString("symbol"),
                rs.getInt("quantity"),
                rs.getDouble("price"),
                TransactionType.valueOf(rs.getString("type")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            tx.setTimestamp(ts.toLocalDateTime());
        } else {
            tx.setTimestamp(LocalDateTime.now());
        }
        return tx;
    }

    // =========================================================================
    // COMPANY methods
    // =========================================================================

    /**
     * Save or update a company profile.
     * Uses INSERT ... ON DUPLICATE KEY UPDATE so it works for both create and update.
     */
    public CompanyProfile saveCompanyProfile(CompanyProfile profile) throws SQLException {
        String sql = """
                INSERT INTO companies (user_id, email, description, industry, status)
                VALUES (?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                  email=VALUES(email),
                  description=VALUES(description),
                  industry=VALUES(industry),
                  status=VALUES(status)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, profile.getUserId());
            ps.setString(2, profile.getEmail());
            ps.setString(3, profile.getDescription());
            ps.setString(4, profile.getIndustry());
            ps.setString(5, profile.getStatus().name());
            ps.executeUpdate();
        }
        return profile;
    }

    /** Find a company profile by user id */
    public Optional<CompanyProfile> findCompanyProfile(int userId) throws SQLException {
        String sql = "SELECT * FROM companies WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapCompanyProfile(rs)) : Optional.empty();
            }
        }
    }

    /** All companies with PENDING status — for admin approval queue */
    public List<CompanyProfile> findPendingCompanies() throws SQLException {
        String sql = "SELECT * FROM companies WHERE status = 'PENDING'";
        List<CompanyProfile> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapCompanyProfile(rs));
        }
        return list;
    }

    /** All approved companies */
    public List<CompanyProfile> findApprovedCompanies() throws SQLException {
        String sql = "SELECT * FROM companies WHERE status = 'APPROVED'";
        List<CompanyProfile> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapCompanyProfile(rs));
        }
        return list;
    }

    // =========================================================================
    // IPO methods
    // =========================================================================

    /**
     * Save a new IPO listing (INSERT only — IPOs are not updated, only status changes).
     * Returns the listing with its generated id set.
     */
    public IpoListing saveIpoListing(IpoListing ipo) throws SQLException {
        String sql = """
                INSERT INTO ipo_listings
                  (company_id, symbol, company_name, shares_offered, shares_remaining,
                   price_per_share, description, deadline, status)
                VALUES (?,?,?,?,?,?,?,?,?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ipo.getCompanyId());
            ps.setString(2, ipo.getSymbol().toUpperCase());
            ps.setString(3, ipo.getCompanyName());
            ps.setInt(4, ipo.getSharesOffered());
            ps.setInt(5, ipo.getSharesRemaining());
            ps.setDouble(6, ipo.getPricePerShare());
            ps.setString(7, ipo.getDescription());
            ps.setDate(8, java.sql.Date.valueOf(ipo.getDeadline()));
            ps.setString(9, ipo.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) ipo.setId(keys.getInt(1));
            }
        }
        return ipo;
    }

    /** Update IPO status and shares_remaining (used when admin approves/rejects or shares are sold) */
    public void updateIpoListing(IpoListing ipo) throws SQLException {
        String sql = """
                UPDATE ipo_listings
                SET status = ?, shares_remaining = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ipo.getStatus().name());
            ps.setInt(2, ipo.getSharesRemaining());
            ps.setInt(3, ipo.getId());
            ps.executeUpdate();
        }
    }

    /** All IPOs with PENDING status — for admin approval */
    public List<IpoListing> findPendingIpos() throws SQLException {
        return queryIpos("WHERE i.status = 'PENDING'");
    }

    /** All IPOs with OPEN status — clients can buy these */
    public List<IpoListing> findOpenIpos() throws SQLException {
        return queryIpos("WHERE i.status = 'OPEN'");
    }

    /** All IPOs for a specific company */
    public List<IpoListing> findIposByCompany(int companyId) throws SQLException {
        return queryIpos("WHERE i.company_id = " + companyId);
    }

    /** All IPOs regardless of status — for admin overview */
    public List<IpoListing> getAllIpos() throws SQLException {
        return queryIpos("");
    }

    /** Find a single IPO by its symbol */
    public Optional<IpoListing> findIpoBySymbol(String symbol) throws SQLException {
        List<IpoListing> list = queryIpos("WHERE i.symbol = '" + symbol.toUpperCase() + "'");
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private List<IpoListing> queryIpos(String whereClause) throws SQLException {
        String sql = """
                SELECT i.*, u.full_name AS company_name
                FROM ipo_listings i
                INNER JOIN users u ON u.id = i.company_id
                """ + whereClause + " ORDER BY i.created_at DESC";
        List<IpoListing> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapIpoListing(rs));
        }
        return list;
    }

    // =========================================================================
    // Mappers
    // =========================================================================

    private CompanyProfile mapCompanyProfile(ResultSet rs) throws SQLException {
        return new CompanyProfile(
                rs.getInt("user_id"),
                rs.getString("email"),
                rs.getString("description"),
                rs.getString("industry"),
                RegistrationStatus.valueOf(rs.getString("status")));
    }

    private IpoListing mapIpoListing(ResultSet rs) throws SQLException {
        return new IpoListing(
                rs.getInt("id"),
                rs.getInt("company_id"),
                rs.getString("company_name"),
                rs.getString("symbol"),
                rs.getInt("shares_offered"),
                rs.getInt("shares_remaining"),
                rs.getDouble("price_per_share"),
                rs.getString("description"),
                rs.getDate("deadline").toLocalDate(),
                IpoListing.IpoStatus.valueOf(rs.getString("status")));
    }
}
