package com.backsuend.coucommerce.catalog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.backsuend.coucommerce.catalog.dto.ProductEditRequest;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.dto.ProductResponse;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.enums.ProductListType;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;

public interface ProductService {

	Sort.Order checkBuildSortOrder(String sortBy, String sortDirection);

	String checkCategoryNullCheck(Category cate);

	void checkExistsProduct(long id, long memberId);

	Page<Product> getProductsListType(ProductListType listType, long memberId,
		String keyword, Category cate, Pageable pageable);

	Product getProductsReadType(ProductReadType readType, long id, long memberId);

	Page<ProductResponse> getProducts(ProductListType listType, int page, int pageSize,
		String sort, String sortDirection, long memberId, String keyword, Category cate);

	ProductResponse getRead(ProductReadType productReadType, long id, long memberId);

	ProductResponse getCreate(ProductRequest dto, long memberId);

	ProductResponse getEdit(long id, ProductEditRequest dto, long memberId);

	void getDelete(long id, long memberId);

}