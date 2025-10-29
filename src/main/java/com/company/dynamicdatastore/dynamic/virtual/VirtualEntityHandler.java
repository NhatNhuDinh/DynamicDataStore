package com.company.dynamicdatastore.dynamic.virtual;

import io.jmix.core.LoadContext;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.KeyValueEntity;

import java.util.List;

public interface VirtualEntityHandler<E> {

    /**
     * Load toàn bộ bản ghi của entity ảo này.
     * Ví dụ: GET /api/orders
     */
    List<E> loadAll(LoadContext<E> ctx);

    /**
     * Load một bản ghi theo id cụ thể.
     * Ví dụ: GET /api/orders/{id}
     *
     * @param ctx load context từ Jmix
     * @param id  id mà DataStore lấy từ context.getId()
     */
    E loadOne(LoadContext<E> ctx, Object id);

    /**
     * Dùng cho dataManager.loadValues(...) / ValueLoadContext
     * Trả về dạng KeyValueEntity (giống bảng động, không cần class thật).
     * Ví dụ: SELECT e.id, e.name FROM VirtualOrder e
     */
    List<KeyValueEntity> loadAllKeyValue(ValueLoadContext ctx);
}
