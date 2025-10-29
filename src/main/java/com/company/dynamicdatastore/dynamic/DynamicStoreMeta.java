package com.company.dynamicdatastore.dynamic;

import java.util.HashMap;
import java.util.Map;

public class DynamicStoreMeta {
    private String storeName;
    private Map<String, EntityMeta> entities = new HashMap<>();


    public DynamicStoreMeta(String storeName) {
        this.storeName = storeName;
    }

    public DynamicStoreMeta() {
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Map<String, EntityMeta> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, EntityMeta> entities) {
        this.entities = entities;
    }
}
