package com.company.dynamicdatastore.dynamic.registry;

import com.company.dynamicdatastore.dynamic.DynamicDataStore;
import com.company.dynamicdatastore.dynamic.DynamicEntity;
import com.company.dynamicdatastore.dynamic.virtual.VirtualEntityHandler;
import io.jmix.core.EntityStates;
import io.jmix.core.KeyValueMapper;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry runtime:
 * - Quản lý các DynamicDataStore (storeA, storeB, ...)
 * - Quản lý MetaClass runtime cho entity ảo
 *
 * Quan trọng:
 *   Khi tạo store mới, ta "new DynamicDataStore(name)"
 *   rồi GỌI setter của AbstractDataStore để nhét
 *   keyValueMapper, metadata, metadataTools, entityStates.
 *   => tránh NullPointerException.
 */
@Component("app_DynamicStoreRegistry")
public class DynamicStoreRegistry {

    // storeName -> DynamicDataStore instance
    private final Map<String, DynamicDataStore> stores = new ConcurrentHashMap<>();

    // storeName -> (entityName -> MetaClass)
    private final Map<String, Map<String, MetaClass>> metaClassesByStore = new ConcurrentHashMap<>();

    // lazy providers từ Spring
    private final ObjectProvider<KeyValueMapper> keyValueMapperProvider;
    private final ObjectProvider<Metadata> metadataProvider;
    private final ObjectProvider<MetadataTools> metadataToolsProvider;
    private final ObjectProvider<EntityStates> entityStatesProvider;

    public DynamicStoreRegistry(ObjectProvider<KeyValueMapper> keyValueMapperProvider,
                                ObjectProvider<Metadata> metadataProvider,
                                ObjectProvider<MetadataTools> metadataToolsProvider,
                                ObjectProvider<EntityStates> entityStatesProvider) {
        this.keyValueMapperProvider = keyValueMapperProvider;
        this.metadataProvider = metadataProvider;
        this.metadataToolsProvider = metadataToolsProvider;
        this.entityStatesProvider = entityStatesProvider;
    }

    /**
     * Tạo (nếu chưa có) store runtime với tên storeName.
     * Inject đầy đủ dependency cho nó bằng setter.
     */
    public DynamicDataStore registerStore(String storeName) {
        return stores.computeIfAbsent(storeName, name -> {
            DynamicDataStore ds = new DynamicDataStore(name, this);

            // Đây là chỗ QUAN TRỌNG để tránh NPE:
            ds.setKeyValueMapper(keyValueMapperProvider.getObject());
            ds.setMetadata(metadataProvider.getObject());
            ds.setMetadataTools(metadataToolsProvider.getObject());
            ds.setEntityStates(entityStatesProvider.getObject());

            return ds;
        });
    }

    public DynamicDataStore getStore(String storeName) {
        return stores.get(storeName);
    }

    public void addEntity(String storeName, DynamicEntity meta) {
        registerStore(storeName).registerEntity(meta);
    }

    public void addHandler(String storeName,
                           String entityName,
                           VirtualEntityHandler<?> handler) {
        registerStore(storeName).registerHandler(entityName, handler);
    }

    public void registerMetaClass(String storeName,
                                  String entityName,
                                  MetaClass metaClass) {
        metaClassesByStore
                .computeIfAbsent(storeName, s -> new ConcurrentHashMap<>())
                .put(entityName, metaClass);
    }

    public MetaClass getMetaClass(String storeName, String entityName) {
        Map<String, MetaClass> byEntity = metaClassesByStore.get(storeName);
        return byEntity != null ? byEntity.get(entityName) : null;
    }

    /**
     * Cho DataStoreFactory custom tra cứu.
     */
    public Map<String, DynamicDataStore> getAllStores() {
        return stores;
    }
}
