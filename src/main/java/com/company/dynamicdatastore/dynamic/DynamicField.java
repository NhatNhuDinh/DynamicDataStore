package com.company.dynamicdatastore.dynamic;

public class DynamicField {
    private final String name;
    private final Class<?> javaType;

    public DynamicField(String name, Class<?> javaType) {
        this.name = name;
        this.javaType = javaType;
    }

    public String getName() {
        return name;
    }

    public Class<?> getJavaType() {
        return javaType;
    }
}
