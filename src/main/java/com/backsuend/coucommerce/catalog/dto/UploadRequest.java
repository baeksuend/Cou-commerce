package com.backsuend.coucommerce.catalog.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import org.springframework.web.multipart.MultipartFile;

public class UploadRequest {

	@NotEmpty(message = "이미지는 최소 1개 이상 업로드해야 합니다.")
	private List<MultipartFile> images;

	// getter & setter
	public List<MultipartFile> getImages() {
		return images;
	}

	public void setImages(List<MultipartFile> images) {
		this.images = images;
	}
}