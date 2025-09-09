package com.backsuend.coucommerce.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrder is a Querydsl query type for Order
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrder extends EntityPathBase<Order> {

    private static final long serialVersionUID = 768591702L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrder order = new QOrder("order1");

    public final com.backsuend.coucommerce.common.entity.QBaseTimeEntity _super = new com.backsuend.coucommerce.common.entity.QBaseTimeEntity(this);

    public final StringPath consumerName = createString("consumerName");

    public final StringPath consumerPhone = createString("consumerPhone");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<OrderDetailProduct, QOrderProduct> items = this.<OrderDetailProduct, QOrderProduct>createList("items", OrderDetailProduct.class, QOrderProduct.class, PathInits.DIRECT2);

    public final com.backsuend.coucommerce.auth.entity.QMember member;

    public final com.backsuend.coucommerce.payment.entity.QPayment payment;

    public final StringPath receiverName = createString("receiverName");

    public final StringPath receiverPhone = createString("receiverPhone");

    public final StringPath receiverPostalCode = createString("receiverPostalCode");

    public final StringPath receiverRoadName = createString("receiverRoadName");

    public final EnumPath<OrderStatus> status = createEnum("status", OrderStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QOrder(String variable) {
        this(Order.class, forVariable(variable), INITS);
    }

    public QOrder(Path<? extends Order> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrder(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrder(PathMetadata metadata, PathInits inits) {
        this(Order.class, metadata, inits);
    }

    public QOrder(Class<? extends Order> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.backsuend.coucommerce.auth.entity.QMember(forProperty("member")) : null;
        this.payment = inits.isInitialized("payment") ? new com.backsuend.coucommerce.payment.entity.QPayment(forProperty("payment"), inits.get("payment")) : null;
    }

}

