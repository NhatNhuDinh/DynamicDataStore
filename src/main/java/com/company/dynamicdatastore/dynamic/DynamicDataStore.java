package com.company.dynamicdatastore.dynamic;

import com.company.dynamicdatastore.dynamic.registry.DynamicStoreRegistry;
import com.company.dynamicdatastore.dynamic.virtual.VirtualEntityHandler;
import io.jmix.core.LoadContext;
import io.jmix.core.SaveContext;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.datastore.AbstractDataStore;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.metamodel.model.MetaClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Một instance = một store runtime (ví dụ "storeA").
 * DataManager.store("storeA") sẽ gọi vào đúng instance này.
 *
 * Lưu ý quan trọng:
 * - KHÔNG giữ KeyValueMapper, Metadata... ở đây nữa.
 *   Những cái đó đã có sẵn ở class cha AbstractDataStore.
 * - Ta sẽ inject thủ công qua setter từ Registry.
 */

public class DynamicDataStore extends AbstractDataStore {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataStore.class);

    private String storeName;

    // mô tả schema động (optional, để tra cứu)
    private final Map<String, EntityMeta> entities = new ConcurrentHashMap<>();

    // entityName -> handler logic load
    private final Map<String, VirtualEntityHandler<?>> handlers = new ConcurrentHashMap<>();

    // thêm cái này để lấy metaClass động ra
    private final DynamicStoreRegistry registry;

    public DynamicDataStore(String storeName, DynamicStoreRegistry registry) {
        this.storeName = storeName;
        this.registry = registry;
    }



    // cho phép registry đổi tên nếu cần
    @Override
    public String getName() {
        return storeName;
    }

    @Override
    public void setName(String name) {
        this.storeName = name;
    }

    // registry gọi
    public void registerEntity(EntityMeta meta) {
        entities.put(meta.getName(), meta);
    }

    // registry gọi
    public void registerHandler(String entityName, VirtualEntityHandler<?> handler) {
        handlers.put(entityName, handler);
    }

    //====================== LOAD ONE ======================
    @Override
    @Nullable
    protected Object loadOne(LoadContext<?> context) {
        Object id = context.getId();
        Class<Object> entityClass = context.getEntityMetaClass().getJavaClass();
        String entityName = context.getEntityMetaClass().getName();
        VirtualEntityHandler<Object> handler = getHandler(entityName);
        if (handler == null) {
            log.warn("[{}] no handler for entity {}", storeName, entityName);
            return null;
        }

        Object one = handler.loadOne((LoadContext<Object>) context, id);

        // có thể null
        return one;
    }

    //====================== LOAD LIST ======================
    @Override
    protected List<Object> loadAll(LoadContext<?> context) {
        Class<Object> entityClass = context.getEntityMetaClass().getJavaClass();
        String entityName = context.getEntityMetaClass().getName();
        VirtualEntityHandler<Object> handler = getHandler(entityName);
        if (handler == null) {
            log.warn("[{}] no handler for entity {}", storeName, entityName);
            return Collections.emptyList();
        }

        if (!context.getIds().isEmpty()) {
            List<Object> out = new ArrayList<>();
            for (Object id : context.getIds()) {
                Object one = handler.loadOne((LoadContext<Object>) context, id);
                if (one != null) out.add(one);
            }
            return out;
        }

        return handler.loadAll((LoadContext<Object>) context);
    }




    @Override
    protected long countAll(LoadContext<?> context) {
        return loadAll(context).size();
    }

    //====================== SAVE/DELETE ====================
    @Override
    protected Set<Object> saveAll(SaveContext context) {
        throw new UnsupportedOperationException("DynamicDataStore[" + storeName + "] read-only");
    }

    @Override
    protected Set<Object> deleteAll(SaveContext context) {
        throw new UnsupportedOperationException("DynamicDataStore[" + storeName + "] read-only");
    }

    //====================== loadValues =====================
    /**
     * DataManager.loadValues(...).store("storeA") sẽ đến đây.
     * Ta trả về List<Object> (thường là List<KeyValueEntity>).
     * Sau đó AbstractDataStore sẽ gọi keyValueMapper.mapValues(...) để build KeyValueEntity chuẩn.
     */
    @Override
    protected List<Object> loadAllValues(ValueLoadContext context) {
        String entityName = extractEntityNameFromValueQuery(context);
        VirtualEntityHandler<Object> handler = getHandler(entityName);
        if (handler == null) {
            log.warn("[{}] no handler for entity {} (ValueLoadContext)", storeName, entityName);
            return Collections.emptyList();
        }

        // handler.loadAllKeyValue() -> List<KeyValueEntity>
        return new ArrayList<>(handler.loadAllKeyValue(context));
    }

    @Override
    protected long countAllValues(ValueLoadContext context) {
        String entityName = extractEntityNameFromValueQuery(context);
        VirtualEntityHandler<Object> handler = getHandler(entityName);
        if (handler == null) return 0;
        List<KeyValueEntity> list = handler.loadAllKeyValue(context);
        return list.size();
    }

    /**
     * Parse "select e.id, e.name from VirtualOrder e ..." => "VirtualOrder"
     */
    protected String extractEntityNameFromValueQuery(ValueLoadContext context) {
        if (context.getQuery() == null || context.getQuery().getQueryString() == null)
            return null;

        String jpql = context.getQuery().getQueryString();
        String lower = jpql.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(" from ");
        if (idx < 0) return null;

        String afterFrom = jpql.substring(idx + " from ".length()).trim();
        String[] parts = afterFrom.split("\\s+");
        return parts.length > 0 ? parts[0] : null;
    }

    //====================== TX lifecycle (no-op) ===========
    @Override
    protected Object beginLoadTransaction(boolean joinTransaction) {
        return new Object();
    }

    @Override
    protected Object beginSaveTransaction(boolean joinTransaction) {
        return new Object();
    }

    @Override
    protected void commitTransaction(Object transaction) {
        // no-op
    }

    @Override
    protected void rollbackTransaction(Object transaction) {
        // no-op
    }

    @Override
    protected TransactionContextState getTransactionContextState(boolean isJoinTransaction) {
        return new NoopTxState();
    }

    protected static class NoopTxState implements TransactionContextState {}

    //====================== helper =========================
    @SuppressWarnings("unchecked")
    private <E> VirtualEntityHandler<E> getHandler(String entityName) {
        if (entityName == null) return null;
        return (VirtualEntityHandler<E>) handlers.get(entityName);
    }
}
