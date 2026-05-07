package com.indicium.ui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Table row model for the Identity Manager TableView.
 * Property names must match PropertyValueFactory strings exactly:
 *   "id", "name", "email", "role", "status", "lastLogin"
 */
public class UserRow {

    private final SimpleIntegerProperty id;
    private final SimpleStringProperty  name;
    private final SimpleStringProperty  email;
    private final SimpleStringProperty  role;
    private final SimpleStringProperty  status;
    private final SimpleStringProperty  lastLogin;

    public UserRow(int id, String name, String email,
                   String role, String status, String lastLogin) {
        this.id        = new SimpleIntegerProperty(id);
        this.name      = new SimpleStringProperty(name);
        this.email     = new SimpleStringProperty(email);
        this.role      = new SimpleStringProperty(role);
        this.status    = new SimpleStringProperty(status);
        this.lastLogin = new SimpleStringProperty(lastLogin);
    }

    // ── Getters (matched to PropertyValueFactory keys) ──

    public int    getId()        { return id.get();        }
    public String getName()      { return name.get();      }
    public String getEmail()     { return email.get();     }
    public String getRole()      { return role.get();      }
    public String getStatus()    { return status.get();    }
    public String getLastLogin() { return lastLogin.get(); }

    // ── Property accessors (required by TableView binding) ──

    public SimpleIntegerProperty idProperty()        { return id;        }
    public SimpleStringProperty  nameProperty()      { return name;      }
    public SimpleStringProperty  emailProperty()     { return email;     }
    public SimpleStringProperty  roleProperty()      { return role;      }
    public SimpleStringProperty  statusProperty()    { return status;    }
    public SimpleStringProperty  lastLoginProperty() { return lastLogin; }

    // ── Setters (used by handleSave / handleRevoke) ──

    public void setName(String v)   { name.set(v);   }
    public void setEmail(String v)  { email.set(v);  }
    public void setRole(String v)   { role.set(v);   }
    public void setStatus(String v) { status.set(v); }
}
