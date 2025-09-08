package com.backsuend.coucommerce.catalog.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.catalog.dto.ProductEditRequest;
import com.backsuend.coucommerce.catalog.dto.ProductItemSearchRequest;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.dto.ProductResponse;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.enums.ProductMainDisplay;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.enums.ProductSortType;

public interface ProductService {

	String checkCategoryNullCheck(Category cate);

	void checkExistsProduct(long productId, long memberId);

	Member checkMember(long memberId);

	Product getProductsReadType(ProductReadType readType, Member member, long productId, long memberId);

	Page<ProductResponse> getProductsMain(ProductMainDisplay mainDisplay, int pageSize);

	Page<Product> getProductsListTypeUser(ProductSortType sortType, long memberId,
		String keyword, Category cate, Pageable pageable);

	Page<Product> getProductsListTypeSeller(ProductSortType sortType, Member member,
		String keyword, Category cate, Pageable pageable);

	Page<ProductResponse> getProductsUser(ProductItemSearchRequest req,
		long memberId, Category cate);

	Page<ProductResponse> getProductsSeller(ProductItemSearchRequest req,
		long memberId, Category cate);

	ProductResponse getRead(ProductReadType productReadType, long productId, long memberId);

	ProductResponse getCreate(ProductRequest dto, long memberId, List<MultipartFile> images);

	ProductResponse getEdit(long productId, long memberId, ProductEditRequest dto, List<MultipartFile> file);

	void getDelete(long productId, long memberId);

}
