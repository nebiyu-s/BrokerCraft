package brokercraft.model;

import java.io.Serializable;

public class ClientProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String email;
    private double balance;
    private RegistrationStatus status;
    private Integer assignedBrokerId;

    public ClientProfile() {}

    public ClientProfile(int userId, String email, double balance,
                         RegistrationStatus status, Integer assignedBrokerId) {
        this.userId = userId;
        this.email = email;
        this.balance = balance;
        this.status = status;
        this.assignedBrokerId = assignedBrokerId;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }

    public Integer getAssignedBrokerId() { return assignedBrokerId; }
    public void setAssignedBrokerId(Integer assignedBrokerId) { this.assignedBrokerId = assignedBrokerId; }
}
