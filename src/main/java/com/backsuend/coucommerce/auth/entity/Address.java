package com.backsuend.coucommerce.auth.entity;

import com.backsuend.coucommerce.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author rua
 */

@Entity
@Table(name = "address", indexes = @Index(name = "idx_address_member", columnList = "member_id", unique = true))
public class Address extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // member와 1:1
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @NotBlank
    @Size(max = 10)
    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @NotBlank @Size(max = 100)
    @Column(name = "road_name", nullable = false, length = 100)
    private String roadName;

    @NotBlank @Size(max = 50)
    @Column(name = "detail", nullable = false, length = 50)
    private String detail;
}
