package com.company.dynamicdatastore.dynamic.runtime;

import com.company.dynamicdatastore.dynamic.registry.DynamicStoreRegistry;
import io.jmix.core.DataStore;
import io.jmix.core.impl.DataStoreFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component("app_RuntimeAwareDataStoreFactory")
public class RuntimeAwareDataStoreFactory extends DataStoreFactory {

    private final DynamicStoreRegistry registry;

    public RuntimeAwareDataStoreFactory(DynamicStoreRegistry registry) {
        this.registry = registry;
    }

    @Override
    public DataStore get(String name) {
        DataStore dynamic = registry.getStore(name);
        if (dynamic != null) {
            return dynamic;
        }
        return super.get(name);
    }
}