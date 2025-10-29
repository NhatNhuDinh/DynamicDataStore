package com.company.dynamicdatastore.dynamic;

public class RuntimeFieldDef {
    private final String name;
    private final Class<?> javaType;

    public RuntimeFieldDef(String name, Class<?> javaType) {
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
