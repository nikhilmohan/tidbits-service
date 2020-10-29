package com.nikhilm.hourglass.tidbitsservice.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Category {
    science("science"), technology("technology"), sport("sport"), travel("travel");

    private String value;

    Category(String value)  {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

}
