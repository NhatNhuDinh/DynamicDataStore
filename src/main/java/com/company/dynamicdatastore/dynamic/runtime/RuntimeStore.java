package com.company.dynamicdatastore.dynamic.runtime;

import io.jmix.core.DataStore;
import io.jmix.core.metamodel.model.Store;
import com.company.dynamicdatastore.dynamic.registry.DynamicStoreRegistry;
import io.jmix.core.metamodel.model.StoreDescriptor;

/**
 * Store runtime, không đăng ký trong bean core_Stores.
 * Chỉ tồn tại trong RAM, sau khi app đã lên.
 */
public class RuntimeStore implements Store {

    private final String name;

    public RuntimeStore(String name) { this.name = name; }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public StoreDescriptor getDescriptor() {
        return null;
    }

    @Override
    public boolean isNullsLastSorting() {
        return false;
    }

    @Override
    public boolean supportsLobSortingAndFiltering() {
        return false;
    }

}
