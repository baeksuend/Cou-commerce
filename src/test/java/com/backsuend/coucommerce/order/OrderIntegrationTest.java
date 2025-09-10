package com.backsuend.coucommerce.order;

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

@DisplayName("주문 분할 통합 테스트")
class OrderIntegrationTest extends BaseIntegrationTest {

    @Autowired private ProductRepository productRepository;

    @Test
    @DisplayName("서로 다른 셀러 상품 담고 주문 생성 → 2건 생성")
    void createSplitOrders() throws Exception {
        // sellers
        Member s1 = createMember("s1@ex.com", "pw", Role.SELLER);
        Member s2 = createMember("s2@ex.com", "pw", Role.SELLER);

        Product p1 = productRepository.save(Product.builder().member(s1).name("A").detail("d").price(1000).stock(10)
            .tranPrice(0).category(Category.DIGITAL).visible(true).build());
        Product p2 = productRepository.save(Product.builder().member(s2).name("B").detail("d").price(2000).stock(5)
            .tranPrice(0).category(Category.FASHION).visible(true).build());

        String token = registerAndLogin("buyer@ex.com", "password123", "홍길동", "010-1111-2222");

        CartItem ci1 = CartItem.builder().productId(p1.getId()).productName(p1.getName()).priceAtAdd(p1.getPrice()).quantity(1).detail("opt").build();
        CartItem ci2 = CartItem.builder().productId(p2.getId()).productName(p2.getName()).priceAtAdd(p2.getPrice()).quantity(1).detail("opt").build();

        mockMvc.perform(post("/api/v1/cart").header("Authorization", "Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ci1)))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/cart").header("Authorization", "Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ci2)))
            .andExpect(status().isCreated());

        String orderJson = "{\n"+
            "\"consumerName\":\"홍길동\",\n"+
            "\"consumerPhone\":\"010-0000-0000\",\n"+
            "\"receiverName\":\"홍길동\",\n"+
            "\"receiverRoadName\":\"서울시 어딘가 123\",\n"+
            "\"receiverPhone\":\"010-0000-0000\",\n"+
            "\"receiverPostalCode\":\"04538\"}";

        MvcResult result = mockMvc.perform(post("/api/v1/orders").header("Authorization", "Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(orderJson))
            .andExpect(status().isCreated()).andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("totalOrders").asInt()).isEqualTo(2);
        assertThat(data.get("orders").size()).isEqualTo(2);
    }

    @Test
    @DisplayName("주문 생성 → 결제 → 배송 → 완료")
    void fullHappyPath_complete() throws Exception {
        // seller + product
        Member seller = createMember("ship1@ex.com", "password123", Role.SELLER);
        Product p = productRepository.save(Product.builder().member(seller).name("A").detail("d").price(1200).stock(10)
            .tranPrice(0).category(Category.DIGITAL).visible(true).build());

        // buyer
        String buyerToken = registerAndLogin("orderbuyer@ex.com", "password123", "홍길동", "010-0000-0000");
        // seller login
        String sellerToken = login("ship1@ex.com", "password123");

        // add to cart & create order
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

        // pay
        String payJson = String.format("{\n  \"orderId\": %d,\n  \"cardBrand\": \"KB\",\n  \"amount\": %d,\n  \"simulate\": \"SUCCESS\"\n}", orderId, 1200);
        mockMvc.perform(post("/api/v1/payments").header("Authorization", "Bearer "+buyerToken)
                .contentType(MediaType.APPLICATION_JSON).content(payJson))
            .andExpect(status().isOk());

        // ship
        String shipJson = "{\n  \"trackingNo\": \"T123\",\n  \"carrier\": \"CJ\"\n}";
        mockMvc.perform(patch("/api/v1/seller/orders/{id}/ship", orderId).header("Authorization", "Bearer "+sellerToken)
                .contentType(MediaType.APPLICATION_JSON).content(shipJson))
            .andExpect(status().isOk());

        // complete
        mockMvc.perform(patch("/api/v1/seller/orders/{id}/complete", orderId).header("Authorization", "Bearer "+sellerToken))
            .andExpect(status().isOk());

        // verify buyer sees COMPLETED
        mockMvc.perform(get("/api/v1/orders/{id}", orderId).header("Authorization", "Bearer "+buyerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
}
