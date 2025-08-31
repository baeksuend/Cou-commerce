package com.backsuend.coucommerce.catalog.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.catalog.dto.ProductEditRequest;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.dto.ProductResponse;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.enums.ProductListType;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.common.exception.CustomValidationException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.common.exception.NotFoundException;
import com.backsuend.coucommerce.member.repository.MemberRepository;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "상품 내용 ", description = "상품 등록, 수정, 조회, 삭제 기능")
@Slf4j(topic = "상품 목록, 등록,  수정, 삭제")
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;

	/**
	 * 상품목록의 정렬순서
	 **/
	@Override
	public Sort.Order checkBuildSortOrder(String sort, String sortDir) {
		String sortBy = (sort == null || sort.isEmpty()) ? "createdAt" : sort;
		String sortDirection = (sortDir == null || sortDir.isEmpty()) ? "desc" : sortDir;
		boolean isDesc = "desc".equalsIgnoreCase(sortDirection);
		return isDesc ? Sort.Order.desc(sortBy) : Sort.Order.asc(sortBy);
	}

	/**
	 * 카테고리 null체크, db에 값 비교할때는 string으로 변경해준다.
	 **/
	@Override
	public String checkCategoryNullCheck(Category cate) {
		if (cate != null) {
			return cate.toString();
		} else {
			return null;
		}
	}

	/**
	 ** 본인이 등록한 상품 체크, 상품명과, 아이디로 체크한다.
	 **/
	@Override
	public void checkExistsProduct(long id, long memberId) {
		productRepository.findByDeletedAtIsNullAndIdAndMemberId(id, memberId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "관련 상품이 없습니다."));
	}

	/**
	 ** 해당 아이디의 점근권한을 체크한다.
	 **/
	public Member checkMember(long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("등록된 회원 정보가 없습니다."));
	}

	/**
	 ** ListType 형식을 비교해서 DB 내용 가져오기
	 * USER_LIST_ALL : 비회원 전체목록
	 * USER_LIST_CATEGORY : 비회원 카테고리별 목록
	 * SELLER_LIST_ALL : 판매자(셀러) 전체 목록
	 * ADMIN_LIST_ALL : 전체관리자 목록
	 **/
	@Override
	public Page<Product> getProductsListType(ProductListType listType, long memberId,
		String keyword, Category cate, Pageable pageable) {
		String cateStr = checkCategoryNullCheck(cate);
		return switch (listType) {
			case ProductListType.USER_LIST_ALL ->
				productRepository.findByVisibleIsTrueAndNameOrCategory(keyword, cateStr, pageable);
			case ProductListType.USER_LIST_CATEGORY ->
				productRepository.findByVisibleIsTrueAndCategoryOrNameContaining(cateStr, keyword, pageable);
			case ProductListType.SELLER_LIST_ALL ->
				productRepository.findByMemberIdAndNameOrCategory(memberId, keyword, cateStr, pageable);
			case ProductListType.ADMIN_LIST_ALL ->
				productRepository.findByIdIsNotNullAndMemberIdOrNameOrCategory(memberId, keyword, cateStr,
					pageable);
		};
	}

	/**
	 ** ReadType 형식을 비교해서 DB 내용 가져오기
	 * USER_READ : 사용자 내용
	 * SELLER_READ : 판매자(셀러) 내용
	 * ADMIN_READ : 관리자 내용
	 **/
	@Override
	public Product getProductsReadType(ProductReadType readType, long id, long memberId) {

		return switch (readType) {
			case ProductReadType.USER_READ -> productRepository.findByDeletedAtIsNullAndVisibleIsTrueAndId(id)
				.orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "관련 상품이 없습니다."));
			case ProductReadType.SELLER_READ -> productRepository.findByDeletedAtIsNullAndIdAndMemberId(id, memberId)
				.orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "관련 상품이 없습니다."));
			case ProductReadType.ADMIN_READ -> productRepository.findByDeletedAtIsNullAndId(id)
				.orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "관련 상품이 없습니다."));
		};
	}

	/**
	 ** 상품 목록 가져오기
	 **/
	@Override
	public Page<ProductResponse> getProducts(ProductListType listType, int page, int pageSize,
		String sort, String sortDirection, long memberId, String keyword, Category cate) {

		Sort.Order order = checkBuildSortOrder(sort, sortDirection);
		Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(order));

		//ListType 내용 가져오기
		Page<Product> pageList = getProductsListType(listType, memberId, keyword, cate, pageable);

		List<ProductResponse> dtoPage = pageList.stream().map(ProductResponse::fromEntity)
			.collect(Collectors.toList());
		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	/**
	 ** 상품 내용 가져오기
	 **/
	@Override
	public ProductResponse getRead(ProductReadType productReadType, long id, long memberId) {

		//ReadType 형식별로 상품내용 가져오기
		Product product = getProductsReadType(productReadType, id, memberId);

		return ProductResponse.fromEntity(product);
	}

	/**
	 ** 상품등록하기
	 **/
	@Override
	public ProductResponse getCreate(ProductRequest dto, long memberId) {

		// 회원 정보가져오기
		Member member = checkMember(memberId);

		Product product = dto.toEntity(member);
		Product saved = productRepository.save(product);
		return ProductResponse.fromEntity(saved);
	}

	/**
	 ** 상품  수정하기
	 **/
	@Override
	public ProductResponse getEdit(long id, ProductEditRequest dto, long memberId) {

		//수정, 삭제시에 본인글 여부 체크
		checkExistsProduct(id, memberId);

		// 회원 정보가져오기
		Member member = checkMember(memberId);

		Product product = dto.toEntity(member);
		Product saved = productRepository.save(product);
		return ProductResponse.fromEntity(saved);
	}

	/**
	 ** 상품 삭제하기
	 **/
	@Override
	public void getDelete(long id, long memberId) {

		//수정, 삭제시에 본인글 여부 체크
		checkExistsProduct(id, memberId);

		Product product = productRepository.findByDeletedAtIsNullAndIdAndMemberId(id, memberId)
			.orElseThrow(() -> new CustomValidationException(ErrorCode.NOT_FOUND, "관련 상품이 없습니다."));
		product.delete();
		productRepository.save(product);
	}

}
