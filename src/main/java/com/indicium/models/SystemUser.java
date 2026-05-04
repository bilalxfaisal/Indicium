package com.indicium.models;
import com.indicium.models.UserRole;
import com.indicium.models.UserAuth;

public abstract class SystemUser
{
    private UserAuth authentication;

    protected String name;
    protected int userID;
    protected String email;
    protected String credentials;
    protected UserRole role;

    public SystemUser(int userID, String name, String email, String credentials, UserRole role)
    {
        this.name = name;
        this.userID = userID;
        this.email = email;
        this.credentials = credentials;
        this.role = role;
    }
}
