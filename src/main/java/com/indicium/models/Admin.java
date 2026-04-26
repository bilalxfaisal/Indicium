package com.indicium.models;
import com.indicium.models.SystemUser;
import com.indicium.models.UserRole;

public class Admin extends SystemUser
{
    String adminToken;
    public Admin(int userID, String email, String credentials, String adminToken)
    {
        this.adminToken = adminToken;
        super(userID, email, credentials, UserRole.ADMIN);
    }


}
