package com.company.dynamicdatastore.view.dynamicdatastore;

import com.company.dynamicdatastore.dynamic.EntityMeta;
import com.company.dynamicdatastore.dynamic.RuntimeFieldDef;
import com.company.dynamicdatastore.dynamic.registry.DynamicStoreRegistry;
import com.company.dynamicdatastore.dynamic.runtime.DynamicMetaClassFactory;
import com.company.dynamicdatastore.dynamic.virtual.VirtualEntityHandler;
import com.company.dynamicdatastore.entity.User;
import com.company.dynamicdatastore.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.LoadContext;
import io.jmix.core.MetadataTools;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import io.jmix.flowui.view.*;

import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Màn hình test full pipeline runtime:
 *
 * 1. Tạo store động "storeA"
 * 2. Khai báo entity ảo "VirtualOrder" (EntityMeta)
 * 3. Gắn handler runtime cho "VirtualOrder"
 * 4. Tạo MetaClass động cho "VirtualOrder" và đăng ký vào registry
 * 5. Gọi DataManager.loadValues(...).store("storeA") để lấy dữ liệu
 * 6. Bind vào rowsDc -> DataGrid trong XML
 */
@Route(value = "dynamic-data-store", layout = MainView.class)
@ViewController(id = "DynamicDataStore")
@ViewDescriptor(path = "dynamic-data-store.xml")
public class DynamicDataStore extends StandardView {

    @Autowired
    private  DynamicStoreRegistry registry;

    @Autowired
    private DynamicMetaClassFactory dynamicMetaClassFactory;

    @Autowired
    private DataManager dataManager;

    @ViewComponent
    private KeyValueCollectionContainer rowsDc;

    @ViewComponent
    private DataGrid<KeyValueEntity> rowsGrid;
    @Autowired
    private MetadataTools metadataTools;

    @Subscribe
    public void onInit(InitEvent event) {
        setupRuntimeEntity();
        loadRowsFromDynamicStore();
    }

    /**
     * B1-B4:
     * - Tạo storeA (nếu chưa có)
     * - Định nghĩa entity VirtualOrder
     * - Đăng ký handler cho VirtualOrder
     * - Build MetaClass runtime cho VirtualOrder và lưu vào registry
     */
    private void setupRuntimeEntity() {
        // B1. Đảm bảo store runtime tồn tại
        registry.registerStore("storeA");

        // B2. Đăng ký metadata logic của entity ảo (thông tin field kiểu Java)
        EntityMeta virtualOrderMeta = new EntityMeta();
        virtualOrderMeta.setName("VirtualOrder");
        virtualOrderMeta.setAttributes(Map.of(
                "id", UUID.class,
                "name", String.class,
                "amount", BigDecimal.class
        ));
        registry.addEntity("storeA", virtualOrderMeta);

        // B3. Tạo handler runtime cho VirtualOrder
        VirtualEntityHandler<KeyValueEntity> orderHandler = new VirtualEntityHandler<>() {

            /**
             * Dùng cho DataManager.load(...).list()
             * (load entity dạng object, không phải loadValues)
             */
            @Override
            public List<KeyValueEntity> loadAll(LoadContext<KeyValueEntity> ctx) {
                return List.of(
                        kv("id", UUID.randomUUID(),
                                "name", "Demo1",
                                "amount", BigDecimal.valueOf(1000)),
                        kv("id", UUID.randomUUID(),
                                "name", "Demo2",
                                "amount", BigDecimal.valueOf(2000))
                );
            }

            /**
             * Dùng cho DataManager.load(...).id(xxx).one()
             */
            @Override
            public KeyValueEntity loadOne(LoadContext<KeyValueEntity> ctx, Object id) {
                return kv("id", id,
                        "name", "SingleRow",
                        "amount", BigDecimal.valueOf(9999));
            }

            /**
             * QUAN TRỌNG:
             * DataManager.loadValues(...).store("storeA").list()
             * sẽ đi vào DynamicDataStore.loadValues(), và nó sẽ gọi handler.loadAllKeyValue(ctx)
             */
            @Override
            public List<KeyValueEntity> loadAllKeyValue(ValueLoadContext ctx) {
                return List.of(
                        kv("id", UUID.randomUUID(),
                                "name", "KV_Demo1",
                                "amount", BigDecimal.valueOf(1111)),
                        kv("id", UUID.randomUUID(),
                                "name", "KV_Demo2",
                                "amount", BigDecimal.valueOf(2222))
                );
            }

            private KeyValueEntity kv(Object... args) {
                KeyValueEntity e = new KeyValueEntity();
                for (int i = 0; i < args.length; i += 2) {
                    String key = args[i].toString();
                    Object value = args[i + 1];
                    e.setValue(key, value);
                }
                return e;
            }
        };

        // Gắn handler này vào storeA cho entity "VirtualOrder"
        registry.addHandler("storeA", "VirtualOrder", orderHandler);



        // B4. Tạo MetaClass runtime cho VirtualOrder và đăng ký vào registry
        // --> cái này giúp UI biết entity ảo có field gì (id, name, amount)
         MetaClass meta = dynamicMetaClassFactory.buildAndRegisterMetaClass(
                "VirtualOrder",
                List.of(
                        new RuntimeFieldDef("id", UUID.class),
                        new RuntimeFieldDef("name", String.class),
                        new RuntimeFieldDef("amount", BigDecimal.class)
                ),
                "storeA"
        );

        // Xoá hết cột cũ (tránh nhân đôi khi mở lại view)
        rowsGrid.removeAllColumns();

        meta.getProperties().forEach(metaProperty -> {
            String propName = metaProperty.getName();

            rowsGrid
                    .addColumn(entity -> {
                        // entity là KeyValueEntity
                        Object v = entity.getValue(propName);
                        return v != null ? v.toString() : "";
                    })
                    .setHeader(propName.toUpperCase());
        });

    }


    private void loadRowsFromDynamicStore() {

        MetaClass meta = dynamicMetaClassFactory.buildAndRegisterMetaClass(
                "VirtualOrder",
                List.of(
                        new RuntimeFieldDef("id", UUID.class),
                        new RuntimeFieldDef("name", String.class),
                        new RuntimeFieldDef("amount", BigDecimal.class)
                ),
                "storeA"
        );
        // 2. Tạo LoadContext dựa trên MetaClass đó
        LoadContext<Object> ctx = new LoadContext<>(meta);
        ctx.setId("id");
       dataManager.load(ctx);


    }
}
