package com.backsuend.coucommerce.sellerregistration.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSellerRegistration is a Querydsl query type for SellerRegistration
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSellerRegistration extends EntityPathBase<SellerRegistration> {

    private static final long serialVersionUID = 835510976L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSellerRegistration sellerRegistration = new QSellerRegistration("sellerRegistration");

    public final com.backsuend.coucommerce.common.entity.QBaseTimeEntity _super = new com.backsuend.coucommerce.common.entity.QBaseTimeEntity(this);

    public final com.backsuend.coucommerce.auth.entity.QMember approvedBy;

    public final StringPath businessRegistrationNumber = createString("businessRegistrationNumber");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.backsuend.coucommerce.auth.entity.QMember member;

    public final StringPath reason = createString("reason");

    public final EnumPath<SellerRegistrationStatus> status = createEnum("status", SellerRegistrationStatus.class);

    public final StringPath storeName = createString("storeName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSellerRegistration(String variable) {
        this(SellerRegistration.class, forVariable(variable), INITS);
    }

    public QSellerRegistration(Path<? extends SellerRegistration> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSellerRegistration(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSellerRegistration(PathMetadata metadata, PathInits inits) {
        this(SellerRegistration.class, metadata, inits);
    }

    public QSellerRegistration(Class<? extends SellerRegistration> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.approvedBy = inits.isInitialized("approvedBy") ? new com.backsuend.coucommerce.auth.entity.QMember(forProperty("approvedBy")) : null;
        this.member = inits.isInitialized("member") ? new com.backsuend.coucommerce.auth.entity.QMember(forProperty("member")) : null;
    }

}

