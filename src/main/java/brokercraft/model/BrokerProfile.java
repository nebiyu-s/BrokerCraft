package brokercraft.model;

import java.io.Serializable;

public class BrokerProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String department;

    public BrokerProfile() {}

    public BrokerProfile(int userId, String department) {
        this.userId = userId;
        this.department = department;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
