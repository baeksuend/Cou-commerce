package com.backsuend.coucommerce.order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 주문 생성 응답(프론트용 래퍼)
 * - 셀러별로 분할 생성된 주문 목록과 총 개수를 함께 반환
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdersCreateResponse {

    private int totalOrders;
    private List<OrderResponse> orders;

    public static OrdersCreateResponse of(List<OrderResponse> orders) {
        return OrdersCreateResponse.builder()
            .totalOrders(orders == null ? 0 : orders.size())
            .orders(orders)
            .build();
    }
}

