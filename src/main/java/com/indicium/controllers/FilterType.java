package main.java.com.indicium.controllers;

public enum FilterType
{
    DATE_RANGE,     // two dates are entered
    INVESTIGATOR,   // Investigator who created it
    STATUS,         // closed, archived, in progress
    CATEGORY,       // theft, murder etc
    PRIORITY;      // high, medium, low
}
