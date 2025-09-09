package com.backsuend.coucommerce.cart;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.cart.dto.CartItem;
import com.fasterxml.jackson.databind.JsonNode;

@DisplayName("Cart 통합 테스트")
class CartIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("장바구니 담기/조회")
    void addAndGetCart() throws Exception {
        String token = registerAndLogin("c1@ex.com", "password123", "홍길동", "010-0000-0000");
        CartItem ci = CartItem.builder().productId(999L).productName("임시상품").priceAtAdd(1234).quantity(2).detail("opt").build();

        mockMvc.perform(post("/api/v1/cart").header("Authorization", "Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ci)))
            .andExpect(status().isCreated());

        MvcResult res = mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer "+token))
            .andExpect(status().isOk()).andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).get("data");
        assertThat(data.get("items").size()).isEqualTo(1);
        assertThat(data.get("totalPrice").asInt()).isEqualTo(1234*2);
    }

    @Test
    @DisplayName("경로변수 삭제 후 비어있는지 확인")
    void deletePathVariable_thenEmpty() throws Exception {
        String token = registerAndLogin("c2@ex.com", "password123", "홍길동", "010-0000-0000");
        CartItem ci = CartItem.builder().productId(777L).productName("삭제상품").priceAtAdd(500).quantity(1).detail("opt").build();
        mockMvc.perform(post("/api/v1/cart").header("Authorization", "Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ci)))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/cart/{pid}", 777L).header("Authorization", "Bearer "+token))
            .andExpect(status().isOk());

        MvcResult res = mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer "+token))
            .andExpect(status().isOk()).andReturn();
        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).get("data");
        assertThat(data.get("items").size()).isEqualTo(0);
        assertThat(data.get("totalPrice").asInt()).isEqualTo(0);
    }
}
