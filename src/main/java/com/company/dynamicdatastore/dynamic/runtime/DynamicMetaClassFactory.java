package com.company.dynamicdatastore.dynamic.runtime;
import com.company.dynamicdatastore.dynamic.DynamicMetaClass;
import com.company.dynamicdatastore.dynamic.RuntimeFieldDef;
import com.company.dynamicdatastore.dynamic.registry.DynamicStoreRegistry;
import io.jmix.core.Stores;
import io.jmix.core.impl.keyvalue.KeyValueMetaClassFactory;
import io.jmix.core.metamodel.model.MetaClass;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory để tạo MetaClass động cho entity ảo,
 * gán nó vào đúng store runtime, thêm các property động,
 * và đăng ký vào DynamicStoreRegistry.
 */
@Component
public class DynamicMetaClassFactory {

    private final KeyValueMetaClassFactory keyValueMetaClassFactory;
    private final DynamicStoreRegistry dynamicStoreRegistry;

    public DynamicMetaClassFactory(KeyValueMetaClassFactory keyValueMetaClassFactory,
                                   DynamicStoreRegistry dynamicStoreRegistry) {
        this.keyValueMetaClassFactory = keyValueMetaClassFactory;
        this.dynamicStoreRegistry = dynamicStoreRegistry;
    }

    public MetaClass buildAndRegisterMetaClass(String entityName,
                                               List<RuntimeFieldDef> fields,
                                               String storeName) {

        // 1. Tạo KeyValueMetaClass động nhưng với tên entityName của mình
        DynamicMetaClass metaClass = new DynamicMetaClass();
        metaClass.setName(entityName);                 // override name (mặc định là "sys_KeyValueEntity")
        // 2. Add các property động bằng configurer
        KeyValueMetaClassFactory.Configurer configurer =
                keyValueMetaClassFactory.configurer(metaClass);
        for (RuntimeFieldDef f : fields) {
            configurer.addProperty(f.getName(), f.getJavaType());
        }
        dynamicStoreRegistry.registerMetaClass(storeName, entityName, metaClass);

        return metaClass;
    }
}