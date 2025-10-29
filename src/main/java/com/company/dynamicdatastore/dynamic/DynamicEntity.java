package com.company.dynamicdatastore.dynamic;

import java.util.Map;

public class DynamicEntity {
    private String name;                      // Entity name
    private Map<String, Class<?>> attributes; // Fields

    public DynamicEntity() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Class<?>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Class<?>> attributes) {
        this.attributes = attributes;
    }
}
