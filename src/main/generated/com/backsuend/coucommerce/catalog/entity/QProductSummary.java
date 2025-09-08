package com.backsuend.coucommerce.catalog.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductSummary is a Querydsl query type for ProductSummary
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductSummary extends EntityPathBase<ProductSummary> {

    private static final long serialVersionUID = -861917190L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductSummary productSummary = new QProductSummary("productSummary");

    public final com.backsuend.coucommerce.common.entity.QBaseTimeEntity _super = new com.backsuend.coucommerce.common.entity.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> orderCount = createNumber("orderCount", Integer.class);

    public final QProduct product;

    public final NumberPath<Double> reviewAvgScore = createNumber("reviewAvgScore", Double.class);

    public final NumberPath<Integer> reviewCount = createNumber("reviewCount", Integer.class);

    public final NumberPath<Double> reviewTotalScore = createNumber("reviewTotalScore", Double.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Integer> viewCount = createNumber("viewCount", Integer.class);

    public final NumberPath<Integer> zimCount = createNumber("zimCount", Integer.class);

    public QProductSummary(String variable) {
        this(ProductSummary.class, forVariable(variable), INITS);
    }

    public QProductSummary(Path<? extends ProductSummary> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductSummary(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductSummary(PathMetadata metadata, PathInits inits) {
        this(ProductSummary.class, metadata, inits);
    }

    public QProductSummary(Class<? extends ProductSummary> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new QProduct(forProperty("product"), inits.get("product")) : null;
    }

}

