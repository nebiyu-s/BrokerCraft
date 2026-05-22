package brokercraft.model;

import java.io.Serializable;

/**
 * CompanyProfile — extra data for a COMPANY user.
 *
 * Mirrors the 'companies' table.
 * A company starts as PENDING after registration.
 * Admin approves it → status becomes APPROVED → company can submit an IPO.
 */
public class CompanyProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private int    userId;
    private String email;
    private String description;
    private String industry;
    private RegistrationStatus status;   // reuse PENDING/APPROVED/REJECTED

    public CompanyProfile() {}

    public CompanyProfile(int userId, String email, String description,
                          String industry, RegistrationStatus status) {
        this.userId      = userId;
        this.email       = email;
        this.description = description;
        this.industry    = industry;
        this.status      = status;
    }

    public int    getUserId()      { return userId; }
    public void   setUserId(int v) { this.userId = v; }

    public String getEmail()          { return email; }
    public void   setEmail(String v)  { this.email = v; }

    public String getDescription()         { return description; }
    public void   setDescription(String v) { this.description = v; }

    public String getIndustry()         { return industry; }
    public void   setIndustry(String v) { this.industry = v; }

    public RegistrationStatus getStatus()          { return status; }
    public void               setStatus(RegistrationStatus v) { this.status = v; }
}
