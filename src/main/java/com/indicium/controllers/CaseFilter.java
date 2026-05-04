package com.indicium.controllers;
import main.java.com.indicium.controllers.FilterType;
import com.indicium.models.Case;
import com.indicium.repository.CaseRepository;
import com.indicium.models.CaseStatus;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CaseFilter
{
    FilterCriteria validatedCriteria;
    CaseRepository caseRepo;

    public CaseFilter()
    {
        caseRepo = new CaseRepository();
    }

    public boolean validate(FilterCriteria criteria)
    {
        FilterType type = criteria.type;
        String value = criteria.value;

        if (type == null || value == null) return false;

        switch (type)
        {
            case STATUS:
                // Check if the input matches your pre-existing CaseStatus enum values
                try {
                    CaseStatus.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.err.println("Validation Failed: Invalid Status.");
                    return false;
                }
                break;

            case PRIORITY:
                if (!value.equalsIgnoreCase("High") &&
                        !value.equalsIgnoreCase("Medium") &&
                        !value.equalsIgnoreCase("Low"))
                    return false;
                break;

            case DATE_RANGE:
                if (!value.matches("\\d{4}-\\d{2}-\\d{2},\\d{4}-\\d{2}-\\d{2}")) {
                    System.err.println("Validation Failed: Date range must be format YYYY-MM-DD,YYYY-MM-DD");
                    return false;
                }

                String[] dates = value.split(",");
                try
                {
                    LocalDate startDate = LocalDate.parse(dates[0]);
                    LocalDate endDate = LocalDate.parse(dates[1]);

                    // Ensure start date isn't after end date
                    if (startDate.isAfter(endDate))
                    {
                        System.err.println("Validation Failed: Start date cannot be later than end date.");
                        return false;
                    }
                }
                catch (DateTimeParseException e)
                {
                    // Catches cases where regex passes but date is invalid (e.g., 2023-13-45 or 2023-02-30)
                    System.err.println("Validation Failed: One or both dates do not exist on the calendar.");
                    return false;
                }
                break;

            case INVESTIGATOR:
            case CATEGORY:
        }

        this.validatedCriteria = new FilterCriteria(type, value);
    }

    private class FilterCriteria
    {
        FilterType type; // like Category
        String value;   // like murder in Category

        public FilterCriteria(FilterType type, String value)
        {
            this.type = type;
            this.value = value;
        }
    }

    public List<Case> buildQuery()
    {
        if (this.validatedCriteria == null)
        {
            return new ArrayList<>();
        }

        String databaseQuery = formatQueryString(validatedCriteria);
        return caseRepo.findByFilter(databaseQuery);
    }

    private String formatQueryString(FilterCriteria criteria)
    {
        // Special formatting for Date Range
        if (criteria.type == FilterType.DATE_RANGE)
        {
            String[] dates = criteria.value.split(",");
            // Formats to: DATE_OPENED BETWEEN '2023-01-01' AND '2023-12-31'
            return String.format("DATE_OPENED BETWEEN '%s' AND '%s'", dates[0], dates[1]);
        }

        // Default formatting for standard exact matches
        return String.format("%s = '%s'", criteria.type.name(), criteria.value);
    }
}
