package com.backsuend.coucommerce.payment;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;

@DisplayName("Payment 통합 테스트")
class PaymentIntegrationTest extends BaseIntegrationTest {

    @Autowired private ProductRepository productRepository;

    @Test
    @DisplayName("주문 생성 후 결제 성공")
    void processPaymentSuccess() throws Exception {
        // sellers & product
        Member s1 = createMember("ps1@ex.com", "pw", Role.SELLER);
        Product p = productRepository.save(Product.builder().member(s1).name("A").detail("d").price(1500).stock(10)
            .tranPrice(0).category(Category.DIGITAL).visible(true).build());

        String token = registerAndLogin("pbuyer@ex.com", "password123", "홍길동", "010-0000-0000");

        // add to cart
        CartItem ci = CartItem.builder().productId(p.getId()).productName(p.getName()).priceAtAdd(p.getPrice()).quantity(2).detail("opt").build();
        mockMvc.perform(post("/api/v1/cart").header("Authorization", "Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ci)))
            .andExpect(status().isCreated());

        // create order
        String orderJson = "{\n"+
            "\"consumerName\":\"홍길동\",\n"+
            "\"consumerPhone\":\"010-0000-0000\",\n"+
            "\"receiverName\":\"홍길동\",\n"+
            "\"receiverRoadName\":\"서울시 어딘가 123\",\n"+
            "\"receiverPhone\":\"010-0000-0000\",\n"+
            "\"receiverPostalCode\":\"04538\"}";

        MvcResult or = mockMvc.perform(post("/api/v1/orders").header("Authorization", "Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(orderJson))
            .andExpect(status().isCreated()).andReturn();
        JsonNode ordersData = objectMapper.readTree(or.getResponse().getContentAsString()).get("data");
        long orderId = ordersData.get("orders").get(0).get("orderId").asLong();

        // process payment
        String payJson = String.format("{\n  \"orderId\": %d,\n  \"cardBrand\": \"KB\",\n  \"amount\": %d,\n  \"simulate\": \"SUCCESS\"\n}", orderId, 1500*2);
        MvcResult pr = mockMvc.perform(post("/api/v1/payments").header("Authorization", "Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(payJson))
            .andExpect(status().isOk()).andReturn();
        JsonNode paymentData = objectMapper.readTree(pr.getResponse().getContentAsString()).get("data");
        assertThat(paymentData.get("status").asText()).isEqualTo("APPROVED");
        assertThat(paymentData.get("orderStatus").asText()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("결제 후 환불 요청 → 셀러 승인 → 주문 REFUNDED")
    void refundFlow() throws Exception {
        Member s1 = createMember("ps2@ex.com", "password123", Role.SELLER);
        Product p = productRepository.save(Product.builder().member(s1).name("A").detail("d").price(1000).stock(10)
            .tranPrice(0).category(Category.DIGITAL).visible(true).build());

        String buyerToken = registerAndLogin("pbuyer2@ex.com", "password123", "홍길동", "010-0000-0000");
        String sellerToken = login("ps2@ex.com", "password123");

        CartItem ci = CartItem.builder().productId(p.getId()).productName(p.getName()).priceAtAdd(p.getPrice()).quantity(1).detail("opt").build();
        mockMvc.perform(post("/api/v1/cart").header("Authorization", "Bearer "+buyerToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ci)))
            .andExpect(status().isCreated());

        String orderJson = "{\n"+
            "\"consumerName\":\"홍길동\",\n"+
            "\"consumerPhone\":\"010-0000-0000\",\n"+
            "\"receiverName\":\"홍길동\",\n"+
            "\"receiverRoadName\":\"서울시 어딘가 123\",\n"+
            "\"receiverPhone\":\"010-0000-0000\",\n"+
            "\"receiverPostalCode\":\"04538\"}";
        MvcResult or = mockMvc.perform(post("/api/v1/orders").header("Authorization", "Bearer "+buyerToken)
                .contentType(MediaType.APPLICATION_JSON).content(orderJson))
            .andExpect(status().isCreated()).andReturn();
        JsonNode ordersData = objectMapper.readTree(or.getResponse().getContentAsString()).get("data");
        long orderId = ordersData.get("orders").get(0).get("orderId").asLong();

        String payJson = String.format("{\n  \"orderId\": %d,\n  \"cardBrand\": \"KB\",\n  \"amount\": %d,\n  \"simulate\": \"SUCCESS\"\n}", orderId, 1000);
        MvcResult pr = mockMvc.perform(post("/api/v1/payments").header("Authorization", "Bearer "+buyerToken)
                .contentType(MediaType.APPLICATION_JSON).content(payJson))
            .andExpect(status().isOk()).andReturn();
        long paymentId = objectMapper.readTree(pr.getResponse().getContentAsString()).get("data").get("paymentId").asLong();

        // buyer refund request
        String refundJson = "{\n  \"reason\": \"단순변심\"\n}";
        mockMvc.perform(post("/api/v1/payments/{pid}/refund-request", paymentId).header("Authorization", "Bearer "+buyerToken)
                .contentType(MediaType.APPLICATION_JSON).content(refundJson))
            .andExpect(status().isOk());

        // seller approve
        mockMvc.perform(patch("/api/v1/seller/orders/{id}/approve-refund", orderId).header("Authorization", "Bearer "+sellerToken))
            .andExpect(status().isOk());

        // verify
        mockMvc.perform(get("/api/v1/orders/{id}", orderId).header("Authorization", "Bearer "+buyerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }
}
