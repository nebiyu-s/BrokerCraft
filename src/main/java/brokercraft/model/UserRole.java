package brokercraft.model;

import java.io.Serializable;

public enum UserRole implements Serializable {
    ADMIN,
    BROKER,
    CLIENT,
    COMPANY   // new: represents a listed or pending company
}
