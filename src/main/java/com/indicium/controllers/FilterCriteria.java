package com.indicium.controllers;

import com.indicium.controllers.FilterType;

public class FilterCriteria
{
    FilterType type; // like Category
    String value;   // like murder in Category

    public FilterCriteria()
    {
        this.type = null;
        this.value = null;
    }
    public FilterCriteria(FilterType type, String value)
    {
        this.type = type;
        this.value = value;
    }
}
