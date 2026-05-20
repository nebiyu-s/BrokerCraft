package brokercraft.service;

import brokercraft.database.DatabaseManager;
import brokercraft.database.Db;
import brokercraft.model.BrokerProfile;
import brokercraft.model.ClientProfile;
import brokercraft.model.RegistrationStatus;
import brokercraft.model.User;
import brokercraft.model.UserRole;

public class AuthService {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public User login(String username, String password, UserRole role) {
        return Db.query(() -> {
            var userOpt = db.findUserByUsername(username);
            if (userOpt.isEmpty()) {
                return null;
            }
            User user = userOpt.get();
            if (!user.getPassword().equals(password) || user.getRole() != role || !user.isActive()) {
                return null;
            }
            if (role == UserRole.CLIENT) {
                var profile = db.findClientProfile(user.getId());
                if (profile.isEmpty() || profile.get().getStatus() != RegistrationStatus.APPROVED) {
                    return null;
                }
            }
            return user;
        });
    }

    public void registerClient(String username, String password, String fullName, String email) {
        Db.execute(() -> {
            if (db.findUserByUsername(username).isPresent()) {
                throw new IllegalArgumentException("Username already exists.");
            }
            User user = new User(0, username, password, fullName, UserRole.CLIENT, false);
            db.saveUser(user);
            ClientProfile profile = new ClientProfile(
                    user.getId(), email, 100_000.0, RegistrationStatus.PENDING, null);
            db.saveClientProfile(profile);
        });
    }

    public User createBroker(String username, String password, String fullName, String department) {
        return Db.query(() -> {
            if (db.findUserByUsername(username).isPresent()) {
                throw new IllegalArgumentException("Username already exists.");
            }
            User broker = new User(0, username, password, fullName, UserRole.BROKER, true);
            db.saveUser(broker);
            db.saveBrokerProfile(new BrokerProfile(broker.getId(), department));
            return broker;
        });
    }

    public void approveClient(int clientId, int brokerId) {
        Db.execute(() -> {
            var profileOpt = db.findClientProfile(clientId);
            var userOpt = db.findUserById(clientId);
            if (profileOpt.isEmpty() || userOpt.isEmpty()) {
                throw new IllegalArgumentException("Client not found.");
            }
            ClientProfile profile = profileOpt.get();
            profile.setStatus(RegistrationStatus.APPROVED);
            profile.setAssignedBrokerId(brokerId);
            db.saveClientProfile(profile);
            User user = userOpt.get();
            user.setActive(true);
            db.saveUser(user);
        });
    }

    public void rejectClient(int clientId) {
        Db.execute(() -> {
            var profileOpt = db.findClientProfile(clientId);
            if (profileOpt.isEmpty()) {
                throw new IllegalArgumentException("Client not found.");
            }
            ClientProfile profile = profileOpt.get();
            profile.setStatus(RegistrationStatus.REJECTED);
            db.saveClientProfile(profile);
        });
    }
}
