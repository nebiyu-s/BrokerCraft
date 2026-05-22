package brokercraft.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * IpoListing — represents one IPO submitted by a company.
 *
 * Mirrors the 'ipo_listings' table.
 *
 * Lifecycle:
 *   Company submits IPO → status = PENDING
 *   Admin approves      → status = OPEN  (stock row created in 'stocks' table)
 *   All shares sold OR deadline passed → status = CLOSED
 *   Admin rejects       → status = REJECTED
 *
 * When status becomes OPEN:
 *   - A row is inserted into 'stocks' so the symbol appears in the live market
 *   - Clients can buy shares via the normal executeTrade() flow
 *   - The company receives the money (tracked via sharesRemaining going down)
 */
public class IpoListing implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum IpoStatus { PENDING, OPEN, CLOSED, REJECTED }

    private int        id;
    private int        companyId;
    private String     companyName;   // denormalized for display
    private String     symbol;
    private int        sharesOffered;
    private int        sharesRemaining;
    private double     pricePerShare;
    private String     description;
    private LocalDate  deadline;
    private IpoStatus  status;

    public IpoListing() {}

    public IpoListing(int id, int companyId, String companyName, String symbol,
                      int sharesOffered, int sharesRemaining, double pricePerShare,
                      String description, LocalDate deadline, IpoStatus status) {
        this.id              = id;
        this.companyId       = companyId;
        this.companyName     = companyName;
        this.symbol          = symbol;
        this.sharesOffered   = sharesOffered;
        this.sharesRemaining = sharesRemaining;
        this.pricePerShare   = pricePerShare;
        this.description     = description;
        this.deadline        = deadline;
        this.status          = status;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int       getId()                    { return id; }
    public void      setId(int v)               { this.id = v; }

    public int       getCompanyId()             { return companyId; }
    public void      setCompanyId(int v)        { this.companyId = v; }

    public String    getCompanyName()           { return companyName; }
    public void      setCompanyName(String v)   { this.companyName = v; }

    public String    getSymbol()                { return symbol; }
    public void      setSymbol(String v)        { this.symbol = v; }

    public int       getSharesOffered()         { return sharesOffered; }
    public void      setSharesOffered(int v)    { this.sharesOffered = v; }

    public int       getSharesRemaining()       { return sharesRemaining; }
    public void      setSharesRemaining(int v)  { this.sharesRemaining = v; }

    public double    getPricePerShare()         { return pricePerShare; }
    public void      setPricePerShare(double v) { this.pricePerShare = v; }

    public String    getDescription()           { return description; }
    public void      setDescription(String v)   { this.description = v; }

    public LocalDate getDeadline()              { return deadline; }
    public void      setDeadline(LocalDate v)   { this.deadline = v; }

    public IpoStatus getStatus()                { return status; }
    public void      setStatus(IpoStatus v)     { this.status = v; }

    /** How many shares have already been sold */
    public int getSoldShares() { return sharesOffered - sharesRemaining; }

    /** Percentage of IPO sold (0-100) */
    public double getSoldPercent() {
        if (sharesOffered == 0) return 0;
        return (getSoldShares() * 100.0) / sharesOffered;
    }
}
