package com.backsuend.coucommerce.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderDetailProduct is a Querydsl query type for OrderDetailProduct
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderDetailProduct extends EntityPathBase<OrderDetailProduct> {

    private static final long serialVersionUID = -1577253976L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderDetailProduct orderDetailProduct = new QOrderDetailProduct("orderDetailProduct");

    public final com.backsuend.coucommerce.common.entity.QBaseTimeEntity _super = new com.backsuend.coucommerce.common.entity.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QOrder order;

    public final NumberPath<Integer> priceSnapshot = createNumber("priceSnapshot", Integer.class);

    public final com.backsuend.coucommerce.catalog.entity.QProduct product;

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QOrderDetailProduct(String variable) {
        this(OrderDetailProduct.class, forVariable(variable), INITS);
    }

    public QOrderDetailProduct(Path<? extends OrderDetailProduct> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderDetailProduct(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderDetailProduct(PathMetadata metadata, PathInits inits) {
        this(OrderDetailProduct.class, metadata, inits);
    }

    public QOrderDetailProduct(Class<? extends OrderDetailProduct> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.order = inits.isInitialized("order") ? new QOrder(forProperty("order"), inits.get("order")) : null;
        this.product = inits.isInitialized("product") ? new com.backsuend.coucommerce.catalog.entity.QProduct(forProperty("product"), inits.get("product")) : null;
    }

}

