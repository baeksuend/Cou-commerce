package com.backsuend.coucommerce.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.catalog.entity.ProductThumbnail;

@Repository
public interface ProductThumbnailRepository extends JpaRepository<ProductThumbnail, Long> {

	List<ProductThumbnail> findByProduct_Id(Long productId);

	void deleteByProduct_Id(Long productId);

}