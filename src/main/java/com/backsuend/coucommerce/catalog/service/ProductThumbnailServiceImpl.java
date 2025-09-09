package com.backsuend.coucommerce.catalog.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.backsuend.coucommerce.catalog.dto.ProductThumbnailDto;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.entity.ProductThumbnail;
import com.backsuend.coucommerce.catalog.repository.ProductThumbnailRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductThumbnailServiceImpl implements ProductThumbnailService {

	private final ProductThumbnailRepository productThumbnailRepository;
	private final Path rootLocation = Paths.get("src/main/resources/static/uploads/products"); // 로컬 저장 폴더
	private final Path rootLocation_delete = Paths.get("src/main/resources/static/"); // 로컬 저장 폴더

	/**
	 * 썸네일 등록
	 **/
	@Override
	public void uploadThumbnail(Product product, List<MultipartFile> images) {
		try {
			// 각 이미지 파일에 대해 업로드 및 DB 저장 수행
			for (MultipartFile image : images) {
				log.info("상품 썸네일 업로드 시작: productId={}, originalFile={}", product.getId(), image.getOriginalFilename());

				// 이미지 파일 경로를 저장
				List<ProductThumbnailDto> fileList = saveImage(product, image);

				for (ProductThumbnailDto dto : fileList) {

					// ProductThumbnail 엔티티 생성 및 저장
					ProductThumbnail thumbnail = new ProductThumbnail(product, dto.getImageType(), dto.getImagePath());
					productThumbnailRepository.save(thumbnail);

					log.debug("썸네일 저장 완료: productId={}, type={}, path={}", product.getId(), dto.getImageType(),
						dto.getImagePath());

				}
			}
		} catch (IOException e) {
			// 파일 저장 중 오류가 발생한 경우 처리
			log.error("상품 썸네일 저장 중 오류 발생: productId={}, error={}", product.getId(), e.getMessage(), e);

		}
	}

	// 이미지 파일을 저장하는 메서드
	@Override
	public List<ProductThumbnailDto> saveImage(Product product, MultipartFile image) throws IOException {

		List<ProductThumbnailDto> imageList = new ArrayList<>();
		String productPath = product.getId().toString();

		// 원본 파일 저장
		String originalFilename = productPath + "/" + UUID.randomUUID() + "_" + image.getOriginalFilename();
		String uploadsDir = "/uploads/products/";
		String dbOriginalFilePath = uploadsDir + originalFilename;
		Path originalFilePath = rootLocation.resolve(originalFilename);

		// 상위 디렉토리 생성
		Files.createDirectories(originalFilePath.getParent());

		// 파일 저장
		try (InputStream is = image.getInputStream()) {
			Files.copy(is, originalFilePath, StandardCopyOption.REPLACE_EXISTING);
		}

		log.debug("원본 이미지 저장 완료: {}", dbOriginalFilePath);

		imageList.add(new ProductThumbnailDto(dbOriginalFilePath, ""));

		// 이미지 읽기 (새 InputStream 사용)
		BufferedImage originalImage;
		try (InputStream is = image.getInputStream()) {
			originalImage = ImageIO.read(is);
		}

		if (originalImage == null) {
			throw new IOException("이미지 읽기 실패: " + image.getOriginalFilename());
		}

		Map<String, Integer> sizes = Map.of(
			"S", 200,
			"M", 350,
			"L", 600
		);

		for (Map.Entry<String, Integer> entry : sizes.entrySet()) {
			String sizeKey = entry.getKey();
			int width = entry.getValue();

			BufferedImage resized = resizeImage(originalImage, width);

			String resizedFilename =
				productPath + "/" + UUID.randomUUID() + "_" + sizeKey + "_" + image.getOriginalFilename();
			String dbFilePath = uploadsDir + resizedFilename;
			Path resizedFilePath = rootLocation.resolve(resizedFilename);

			// 상위 디렉토리 생성
			Files.createDirectories(resizedFilePath.getParent());

			// 파일 저장
			ImageIO.write(resized, "jpg", resizedFilePath.toFile());

			//list에 이미지 저장
			imageList.add(new ProductThumbnailDto(dbFilePath, sizeKey));
			log.debug("리사이즈 이미지 저장 완료: size={}, path={}", sizeKey, dbFilePath);

		}

		return imageList;
	}

	// 이미지 리사이즈 메서드
	@Override
	public BufferedImage resizeImage(BufferedImage originalImage, int targetWidth) {

		int targetHeight = (int)(((double)targetWidth / originalImage.getWidth()) * originalImage.getHeight());
		Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
		BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = output.createGraphics();
		g2d.drawImage(resultingImage, 0, 0, null);
		g2d.dispose();
		return output;
	}

	/**
	 * 상품 삭제
	 * @param productId 상품 아이디
	 */
	@Transactional
	@Override
	public void deleteProductImages(Long productId) {

		log.info("상품 이미지 삭제 요청: productId={}", productId);

		List<ProductThumbnail> images = productThumbnailRepository.findByProduct_Id(productId);

		for (ProductThumbnail img : images) {

			String relativePath = img.getImagePath(); // "/uploads/products/26/thumbnails/..."
			Path filePath = rootLocation_delete.resolve(relativePath.substring(1)); // 앞 / 제거

			File thumb = filePath.toFile();

			// 파일 삭제
			if (thumb.exists()) {
				boolean deleted = thumb.delete();

				//db에서 이미지 삭제
				productThumbnailRepository.delete(img);

				if (!deleted) {
					log.debug("파일 삭제 성공: {}", thumb.getAbsolutePath());
				} else {
					log.error("파일 삭제 실패: {}", thumb.getAbsolutePath());
				}
			} else {
				log.warn("삭제할 파일이 존재하지 않음: {}", thumb.getAbsolutePath());
			}
		}
		log.info("상품 이미지 삭제 완료: productId={}", productId);
	}
}
