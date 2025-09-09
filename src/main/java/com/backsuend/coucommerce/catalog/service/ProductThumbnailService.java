package com.backsuend.coucommerce.catalog.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.backsuend.coucommerce.catalog.dto.ProductThumbnailDto;
import com.backsuend.coucommerce.catalog.entity.Product;

public interface ProductThumbnailService {
	void uploadThumbnail(Product product, List<MultipartFile> images);

	List<ProductThumbnailDto> saveImage(Product product, MultipartFile image) throws IOException;

	BufferedImage resizeImage(BufferedImage originalImage, int targetWidth);

	void deleteProductImages(Long productId);

}
