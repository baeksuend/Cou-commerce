package com.backsuend.coucommerce.catalog.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.backsuend.coucommerce.catalog.dto.ProductThumbnailDto;
import com.backsuend.coucommerce.catalog.dto.UploadRequest;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.entity.ProductThumbnail;
import com.backsuend.coucommerce.catalog.repository.ProductThumbnailRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductThumbnailServiceImpl implements ProductThumbnailService {

	private final ProductThumbnailRepository productThumbnailRepository;
	private final String uploadsDir = "uploads";
	private final Path rootLocation = Paths.get(uploadsDir); // 로컬 저장 폴더

	/**
	 * 썸네일 등록
	 **/
	@Override
	public void uploadThumbnail(Product product, UploadRequest upload) {
		try {
			// 이미지 파일 저장을 위한 경로 설정
			//String uploadsDir = "src/main/resources/static/uploads/thumbnails/";

			//이미지 삭제 처리
			deleteProductImages(product.getId());

			// 각 이미지 파일에 대해 업로드 및 DB 저장 수행
			for (MultipartFile image : upload.getImages()) {

				// 이미지 파일 경로를 저장
				List<ProductThumbnailDto> fileList = saveImage(product, image);

				for (ProductThumbnailDto dto : fileList) {

					// ProductThumbnail 엔티티 생성 및 저장
					ProductThumbnail thumbnail = new ProductThumbnail(product, dto.getImageType(), dto.getImagePath());
					productThumbnailRepository.save(thumbnail);

				}
			}
		} catch (IOException e) {
			// 파일 저장 중 오류가 발생한 경우 처리
			e.printStackTrace();
		}
	}

	// 이미지 파일을 저장하는 메서드
	@Override
	public List<ProductThumbnailDto> saveImage(Product product, MultipartFile image) throws IOException {

		List<ProductThumbnailDto> imageList = new ArrayList<>();
		String product_path = "/" + product.getId();

		// 원본 파일 저장
		String originalFilename = product_path + "/" + UUID.randomUUID() + "_" + image.getOriginalFilename();
		String dbOriginalFilePath = uploadsDir + originalFilename;
		Path filePath = rootLocation.resolve(originalFilename);
		Files.copy(image.getInputStream(), filePath);

		//이미지 list에 원본이미지 넣기
		imageList.add(new ProductThumbnailDto(dbOriginalFilePath, ""));

		// 이미지 리사이징 후 S, M, L 생성 (예: 썸네일, 중간, 원본)
		BufferedImage originalImage = ImageIO.read(image.getInputStream());

		Map<String, Integer> sizes = Map.of(
			"S", 200,
			"M", 350,
			"L", 600
		);

		for (Map.Entry<String, Integer> entry : sizes.entrySet()) {
			String sizeKey = entry.getKey();
			int width = entry.getValue();

			// 리사이즈
			BufferedImage resized = resizeImage(originalImage, width);
			String resizedFilename =
				product_path + "/thumbnails/" + UUID.randomUUID() + "_" + sizeKey + "_" + image.getOriginalFilename();
			String dbFilePath = uploadsDir + resizedFilename;
			File outputFile = rootLocation.resolve(resizedFilename).toFile();
			ImageIO.write(resized, "jpg", outputFile);

			//이미지 list에 썸네일 이미지 넣기
			imageList.add(new ProductThumbnailDto(dbFilePath, sizeKey));
/*
			// DB 저장
			ProductImage image = ProductImage.builder()
				.size(sizeKey)
				.url("/uploads/thumbnails/" + resizedFilename)
				.build();

			product.addImage(image);*/
		}
		return imageList;
/*
		// 파일 이름 생성
		String fileName = UUID.randomUUID().toString().replace("-", "") + "_" + image.getOriginalFilename();
		// 실제 파일이 저장될 경로
		String filePath = uploadsDir + fileName;
		// DB에 저장할 경로 문자열
		String dbFilePath = "/uploads/thumbnails/" + fileName;

		Path path = Paths.get(filePath); // Path 객체 생성
		Files.createDirectories(path.getParent()); // 디렉토리 생성
		Files.write(path, image.getBytes()); // 디렉토리에 파일 저장

		return dbFilePath;*/
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
	 * @param productId
	 * @return void
	 */
	@Transactional
	@Override
	public void deleteProductImages(Long productId) {

		List<ProductThumbnail> images = productThumbnailRepository.findByProduct_Id(productId);

		for (ProductThumbnail img : images) {
			// 파일 삭제
			File thumb = new File(img.getImagePath());
			if (thumb.exists())
				thumb.delete();
		}
		// DB 레코드 삭제
		productThumbnailRepository.deleteByProduct_Id(productId);

/*		for (String size : sizes) {
			File thumb = new File(uploadDir + filename.replace(".jpg", "_" + size + ".jpg"));
			if (thumb.exists())
				thumb.delete();
		}

		// 원본 파일 삭제
		File original = new File(uploadDir + filename);
		if (original.exists())
			original.delete();*/

	}

	/**
	 * 상품 등록
	 * @param requestDto
	 * @return productId
	 */
/*
	public Long createProduct(ProductCreateDto requestDto, List<MultipartFile> images) {
		if (requestDto.getPrice() < 0) {
			throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
		}
		// DTO를 엔티티로 매핑
		Product product = modelMapper.map(requestDto, Product.class);
		productRepositoryV1.save(product);

		// 추가 - 썸네일 저장 메서드 실행
		productThumbnailService.uploadThumbnail(product, images);

		return product.getProductId();
	}*/

}
