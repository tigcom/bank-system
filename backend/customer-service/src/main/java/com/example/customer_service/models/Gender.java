package com.example.customer_service.models;


import com.fasterxml.jackson.annotation.JsonCreator;

public enum Gender {
    male, female, other;

    @JsonCreator
    public static Gender fromString(String value) {
        return value == null ? null : Gender.valueOf(value.toLowerCase());
    }
}



