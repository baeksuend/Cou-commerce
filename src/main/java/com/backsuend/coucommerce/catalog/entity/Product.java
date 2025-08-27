package com.backsuend.coucommerce.catalog.entity;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author rua
 */
@Entity
@Table(name = "product",
        indexes = {
                @Index(name = "idx_product_member", columnList = "member_id"),
                @Index(name = "idx_product_category", columnList = "category"),
                @Index(name = "idx_product_visible", columnList = "is_status")
        })
public class Product extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SELLER 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member seller;

    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotBlank
    @Lob
    @Column(name = "detail", nullable = false)
    private String detail;

    @Min(0)
    @Column(name = "stock", nullable = false)
    private int stock;

    @Min(0)
    @Column(name = "price", nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private Category category;

    /** 공개 여부 (is_status) */
    @Column(name = "is_status", nullable = false)
    private boolean visible = true;

}
