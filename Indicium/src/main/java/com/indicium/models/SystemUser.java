package com.indicium.models;
import com.indicium.models.UserRole;

public abstract class SystemUser
{
    private UserAuth authentication;

    protected int userID;
    protected String email;
    protected String credentials;
    protected UserRole role;

    public SystemUser(int userID, String email, String credentials, UserRole role)
    {
        this.userID = userID;
        this.email = email;
        this.credentials = credentials;
        this.role = role;
    }
}
