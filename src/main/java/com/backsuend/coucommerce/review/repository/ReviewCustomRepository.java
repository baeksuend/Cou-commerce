package com.backsuend.coucommerce.review.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.review.entity.Review;

public interface ReviewCustomRepository {

	Optional<Review> findByProductAndId(Product product, Long comment_id);

	Page<Review> findByProductAndParentReviewIsNull(Product product, Pageable pageable); //AndParentCommentIsNull

}
