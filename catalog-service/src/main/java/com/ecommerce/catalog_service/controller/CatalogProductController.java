package com.ecommerce.catalog_service.controller;

import com.ecommerce.catalog_service.dto.ProductFilterRequest;
import com.ecommerce.catalog_service.dto.ProductResponse;
import com.ecommerce.catalog_service.service.CatalogProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
public class CatalogProductController {

    private static Logger logger = LogManager.getLogger(CatalogProductController.class);

    private final CatalogProductService catalogProductService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(@ModelAttribute @Valid ProductFilterRequest request) {
        logger.info("Received request to get products with filter: {}", request);

        return ResponseEntity.ok(catalogProductService.getProducts(request));
    }
}
