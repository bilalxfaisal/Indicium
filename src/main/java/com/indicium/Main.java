package com.indicium;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;



public class Main
{
    public static void main(String[] args) throws Exception
    {

        // Change this to whatever password you want
        String password = "ADMIN1234";

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encoded = digest.digest(password.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : encoded) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }

        System.out.println("Password : " + password);
        System.out.println("SHA-256  : " + hex);
    }
}
