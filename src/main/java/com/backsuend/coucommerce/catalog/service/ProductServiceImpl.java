package com.backsuend.coucommerce.catalog.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.catalog.dto.ProductEditRequest;
import com.backsuend.coucommerce.catalog.dto.ProductItemSearchRequest;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.dto.ProductResponse;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.entity.ProductSummary;
import com.backsuend.coucommerce.catalog.entity.ProductThumbnail;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.enums.ProductMainDisplay;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.enums.ProductSortType;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.catalog.repository.ProductThumbnailRepository;
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
	private final ProductThumbnailRepository productThumbnailRepository;
	private final ProductSummaryService productSummaryService;
	private final ProductThumbnailServiceImpl productThumbnailService;

	/**
	 * 카테고리 null체크, db에 값 비교할때는 string으로 변경해준다.
	 **/
	@Override
	public String checkCategoryNullCheck(Category cate) {

		log.debug("checkCategoryNullCheck 호출: {}", cate);
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
	public void checkExistsProduct(long productId, long memberId) {

		log.debug("checkMember 호출: productId={}, memberId={}", productId, memberId);

		productRepository.findByDeletedAtIsNullAndIdAndMember_Id(productId, memberId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "관련 상품이 없습니다.1"));
	}

	/**
	 ** 해당 아이디의 점근권한을 체크한다.
	 **/
	@Override
	public Member checkMember(long memberId) {

		log.debug("checkMember 호출: memberId={}", memberId);

		return memberRepository.findById(memberId)
			.orElseThrow(() -> new CustomValidationException(ErrorCode.VALIDATION_FAILED, "등록된 회원 정보가 없습니다."));
	}

	@Override
	public Page<Product> getProductsListTypeUser(ProductSortType sortType, long memberId,
		String keyword, Category cate, Pageable pageable) {

		log.debug("getProductsListTypeUser 호출: sortType={}, memberId={}, keyword={}, category={}, pageable={}",
			sortType, memberId, keyword, cate, pageable);

		return switch (sortType) {
			case RECENT -> productRepository.userListCategory_RECENT(cate, keyword, pageable);
			case LOW_PRICE -> productRepository.userListCategory_LOW_PRICE(cate, keyword, pageable);
			case HIGH_PRICE -> productRepository.userListCategory_HIGH_PRICE(cate, keyword, pageable);
			case SALE_COUNT_TOTAL -> productRepository.userListCategory_SALE_COUNT_TOTAL(cate, keyword, pageable);
			case REVIEW_SCORE_TOTAL -> productRepository.userListCategory_REVIEW_SCORE_TOTAL(cate, keyword, pageable);
		};
	}

	@Override
	public Page<Product> getProductsListTypeSeller(ProductSortType sortType, Member member,
		String keyword, Category cate, Pageable pageable) {

		log.debug("getProductsListTypeSeller 호출: sortType={}, member={}, keyword={}, category={}, pageable={}",
			sortType, member, keyword, cate, pageable);

		return switch (sortType) {
			case RECENT -> productRepository.sellerListCategory_RECENT(member, keyword, cate, pageable);
			case LOW_PRICE -> productRepository.userListCategory_LOW_PRICE(cate, keyword, pageable);
			case HIGH_PRICE -> productRepository.userListCategory_HIGH_PRICE(cate, keyword, pageable);
			case SALE_COUNT_TOTAL -> productRepository.userListCategory_SALE_COUNT_TOTAL(cate, keyword, pageable);
			case REVIEW_SCORE_TOTAL -> productRepository.userListCategory_REVIEW_SCORE_TOTAL(cate, keyword, pageable);
		};
	}

	/**
	 ** ReadType 형식을 비교해서 DB 내용 가져오기
	 * USER_READ : 사용자 내용
	 * SELLER_READ : 판매자(셀러) 내용
	 **/
	@Override
	public Product getProductsReadType(ProductReadType readType, Member member, long productId, long memberId) {

		log.debug("getProductsReadType 호출: readType={}, productId={}, memberId={}",
			readType, productId, memberId);

		return switch (readType) {
			case ProductReadType.USER_READ -> productRepository.findUserRead(productId)
				.orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "관련 상품이 없습니다."));
			case ProductReadType.SELLER_READ -> productRepository.findSellerRead(member, productId)
				.orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "관련 상품이 없습니다."));
		};
	}

	/**
	 ** 상품 목록 가져오기
	 **/
	@Transactional
	@Override
	public Page<ProductResponse> getProductsMain(ProductMainDisplay mainDisplay, int pageSize) {

		log.info("getProductsMain 호출: mainDisplay={}, pageSize={}", mainDisplay, pageSize);

		Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Order.desc("createdAt")));
		Page<Product> pageList;
		if (mainDisplay.equals(ProductMainDisplay.MAIN_BEST)) {
			pageList = productRepository.searchMainBestProducts(pageable);
		} else {
			pageList = productRepository.searchMainManyReviewProducts(pageable);
		}
		List<ProductResponse> dtoPage = pageList.stream().map(ProductResponse::fromEntity)
			.collect(Collectors.toList());

		// 최대 10개만 잘라서 반환
		if (dtoPage.size() > pageSize) {
			dtoPage = dtoPage.subList(0, pageSize);
		}

		log.debug("getProductsMain 결과: dtoPageSize={}, totalElements={}",
			dtoPage.size(), pageList.getTotalElements());

		return new PageImpl<>(dtoPage, pageable, Math.min(pageList.getTotalElements(), pageSize));
	}

	@Transactional
	@Override
	public Page<ProductResponse> getProductsUser(ProductItemSearchRequest req,
		long memberId, Category cate) {

		log.info("getProductsUser 호출: memberId={}, category={}, page={}, pageSize={}",
			memberId, cate, req.getPage(), req.getPageSize());

		Sort.Order order = Sort.Order.desc("createdAt");
		Pageable pageable = PageRequest.of(req.getPage() - 1,
			req.getPageSize(), Sort.by(order));

		//ListType 내용 가져오기
		Page<Product> pageList = getProductsListTypeUser(req.getSort(), memberId, req.getKeyword(), cate, pageable);

		List<ProductResponse> dtoPage = pageList.stream().map(ProductResponse::fromEntity)
			.collect(Collectors.toList());

		log.debug("getProductsUser 결과: pageSize={}, pageable={}, totalElements={}",
			dtoPage.size(), pageable, pageList.getTotalElements());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	@Transactional
	@Override
	public Page<ProductResponse> getProductsSeller(ProductItemSearchRequest req,
		long memberId, Category cate) {

		log.info("getProductsSeller 호출: req={}, memberId={}, cate={}",
			req, memberId, cate);

		Sort.Order order = Sort.Order.desc("createdAt");
		Pageable pageable = PageRequest.of(req.getPage() - 1,
			req.getPageSize(), Sort.by(order));

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new CustomValidationException(ErrorCode.ACCESS_DENIED, "접근이 불가합니다."));

		//ListType 내용 가져오기
		Page<Product> pageList = getProductsListTypeSeller(req.getSort(), member, req.getKeyword(), cate, pageable);

		List<ProductResponse> dtoPage = pageList.stream().map(ProductResponse::fromEntity)
			.collect(Collectors.toList());

		log.debug("getProductsSeller 결과: pageSize={}, pageable={}, totalElements={}",
			dtoPage.size(), pageable, pageList.getTotalElements());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	/**
	 ** 상품 내용 가져오기
	 **/
	@Override
	public ProductResponse getRead(ProductReadType productReadType, long productId, long memberId) {

		log.info("getRead 호출: productReadType={}, productId={}, memberId={}",
			productReadType, productId, memberId);

		Member member = memberRepository.findById(memberId).orElse(null);
		//ReadType 형식별로 상품내용 가져오기
		Product product = getProductsReadType(productReadType, member, productId, memberId);

		//** 추가 productSummary에 상품조회수 업데이트 / viewCount +1
		productSummaryService.setViewCount(product.getId());

		log.debug("getProductsSeller 결과: product id={}", product.getId());

		return ProductResponse.fromEntity(product);
	}

	/**
	 ** 상품등록하기
	 **/
	@Override
	public ProductResponse getCreate(ProductRequest dto, long memberId, List<MultipartFile> images) {

		log.info("getCreate 호출: memberId={}, productName={}", memberId, dto.getName());

		Member member = checkMember(memberId);
		Product product = dto.toEntity(member);

		ProductSummary summary = new ProductSummary();
		summary.setProduct(product);  // ProductSummary가 외래키 주인
		product.setProductSummary(summary);

		Product saved = productRepository.save(product);

		//** 추가 - 썸네일 저장 메서드 실행
		productThumbnailService.uploadThumbnail(product, images);

		//저장된 내용 가져오기
		List<ProductThumbnail> productThumbnail = productThumbnailRepository.findByProduct_Id(saved.getId());
		saved.setProductThumbnails(productThumbnail);

		log.info("getCreate 완료: productId={}, thumbnails={}", saved.getId(), productThumbnail.size());

		return ProductResponse.fromEntity(saved);
	}

	/**
	 ** 상품  수정하기
	 **/
	@Override
	public ProductResponse getEdit(long productId, long memberId, ProductEditRequest dto, List<MultipartFile> images) {

		log.info("getEdit 호출: productId={}, memberId={}", productId, memberId);

		//수정, 삭제시에 본인글 여부 체크
		checkExistsProduct(productId, memberId);

		//기존 등록 상품 조회
		Product product = productRepository.findById(productId)
			.orElseThrow(() -> new RuntimeException("등록된 상품이 없음11"));

		//내용복사
		BeanUtils.copyProperties(dto, product, "id", "productSummary");

		//수정
		Product saved = productRepository.save(product);

		//** (추가) 기존에 이미지가 있고 선택된 이미지 있으면 삭제
		boolean exists = productThumbnailRepository.existsByProduct_Id(productId);
		if (exists && !images.isEmpty()) {
			productThumbnailService.deleteProductImages(product.getId());
		}

		//** 추가 - 썸네일 저장 메서드 실행
		productThumbnailService.uploadThumbnail(product, images);

		log.info("새 썸네일 업로드 완료: productId={}, imagesCount={}", product.getId(), images.size());

		return ProductResponse.fromEntity(saved);
	}

	/**
	 ** 상품 삭제하기
	 **/
	@Override
	public void getDelete(long productId, long memberId) {

		log.info("getDelete 호출: productId={}, memberId={}", productId, memberId);

		//수정, 삭제시에 본인글 여부 체크
		checkExistsProduct(productId, memberId);

		Member member = memberRepository.findById(memberId).orElse(null);
		Product product = productRepository.findSellerRead(member, productId)
			.orElseThrow(() -> new CustomValidationException(ErrorCode.NOT_FOUND, "관련 상품이 없습니다.2"));

		//삭제처리
		product.delete();

		//** (추가) 보관을 위해서 이미지 삭제 X

		//deletedAt 날짜 등록후 저장
		productRepository.save(product);

		log.info("상품 삭제 처리 완료: productId={}", productId);

	}

}
