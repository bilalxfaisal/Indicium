package com.indicium.models;

public class Investigator
{
    private int investigatorID;
    private String name;
    private String badgeNumber;
    private String rank;
    private String clearanceLevel; // e.g., "Standard", "Admin", "Lead"

    // Constructor
    public Investigator(int investigatorID, String name, String badgeNumber, String rank, String clearanceLevel) {
        this.investigatorID = investigatorID;
        this.name = name;
        this.badgeNumber = badgeNumber;
        this.rank = rank;
        this.clearanceLevel = clearanceLevel;
    }

    // Getters
    public int getInvestigatorID() { return investigatorID; }
    public String getName() { return name; }
    public String getBadgeNumber() { return badgeNumber; }
    public String getRank() { return rank; }
    public String getClearanceLevel() { return clearanceLevel; }

    // Setters
    public void setRank(String rank) { this.rank = rank; }
    public void setClearanceLevel(String clearanceLevel) { this.clearanceLevel = clearanceLevel; }

    @Override
    public String toString() {
        return rank + " " + name + " (" + badgeNumber + ")";
    }
}