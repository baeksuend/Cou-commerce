package com.backsuend.coucommerce.catalog.entity;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.common.entity.BaseTimeEntity;
import jakarta.persistence.*;

/**
 * @author rua
 */

@Entity
@Table(name = "review",
        indexes = {
                @Index(name = "idx_review_member", columnList = "member_id"),
                @Index(name = "idx_review_product", columnList = "product_id")
        })
public class Review extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 작성자
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Lob
    @Column(name = "content")
    private String content;
}
