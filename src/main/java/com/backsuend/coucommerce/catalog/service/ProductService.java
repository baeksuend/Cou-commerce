package com.backsuend.coucommerce.catalog.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.catalog.dto.ProductEditRequest;
import com.backsuend.coucommerce.catalog.dto.ProductItemSearchRequest;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.dto.ProductResponse;
import com.backsuend.coucommerce.catalog.dto.UploadRequest;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.enums.ProductListType;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.enums.ProductSortType;

public interface ProductService {

	Sort.Order checkBuildSortOrder(ProductSortType sort);

	String checkCategoryNullCheck(Category cate);

	void checkExistsProduct(long productId, long memberId);

	Member checkMember(long memberId);
	
	Page<Product> getProductsListType(ProductListType listType, long memberId,
		String keyword, Category cate, Pageable pageable);

	Product getProductsReadType(ProductReadType readType, long productId, long memberId);

	//	Page<ProductResponse> getProducts(ProductListType listType, int page, int pageSize,
	//		String sort, String sortDirection, long memberId, String keyword, Category cate);

	List<ProductResponse> getProductsMain(ProductSortType sort);

	Page<ProductResponse> getProducts(ProductListType listType, ProductItemSearchRequest req,
		long memberId, Category cate);

	ProductResponse getRead(ProductReadType productReadType, long productId, long memberId);

	ProductResponse getCreate(ProductRequest dto, long memberId, UploadRequest upload);

	ProductResponse getEdit(long productId, long memberId, ProductEditRequest dto, UploadRequest file);

	void getDelete(long productId, long memberId);

}
