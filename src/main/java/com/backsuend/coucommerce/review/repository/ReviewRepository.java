package com.backsuend.coucommerce.review.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.review.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewCustomRepository {   //

	List<Review> findByProduct_id(Long product_id, Pageable pageable); // Iterable → ArrayList 수정

	Optional<Review> findByProduct_idAndId(Long product_id, Long id);

}
