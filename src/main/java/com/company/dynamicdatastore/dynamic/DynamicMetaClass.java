package com.company.dynamicdatastore.dynamic;

import io.jmix.core.impl.keyvalue.KeyValueMetaClass;

public class DynamicMetaClass extends KeyValueMetaClass {

    public void setName(String name) {
        // 'name' là protected trong MetadataObjectImpl, nên ta gán trực tiếp được
        this.name = name;
    }
}
