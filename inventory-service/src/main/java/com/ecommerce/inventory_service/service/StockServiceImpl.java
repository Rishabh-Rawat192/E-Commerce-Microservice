package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.StockResponse;
import com.ecommerce.inventory_service.dto.StockStatusUpdatedEvent;
import com.ecommerce.inventory_service.dto.StockUpdateRequest;
import com.ecommerce.inventory_service.entity.Inventory;
import com.ecommerce.inventory_service.exception.ApiException;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private static final Logger logger = LogManager.getLogger(StockServiceImpl.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryProducerService inventoryProducerService;

    @Override
    public StockResponse restock(StockUpdateRequest request, UUID productId, UUID sellerId) {
        Inventory inventory = getAuthorizedInventory(productId, sellerId);

        boolean wasOutOfStock = inventory.getTotalQuantity() == 0;

        inventory.setTotalQuantity(inventory.getTotalQuantity() + request.stock());
        inventoryRepository.save(inventory);

        if (wasOutOfStock)
            inventoryProducerService.sendStockStatusUpdatedEvent(new StockStatusUpdatedEvent(productId, true));

        logger.info("Restocked productId={} by sellerId={} with quantity={}", productId, sellerId, request.stock());
        return StockResponse.from(inventory);
    }

    @Override
    public StockResponse deductStock(StockUpdateRequest request, UUID productId, UUID sellerId) {
        Inventory inventory = getAuthorizedInventory(productId, sellerId);

        int availableStocks = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        if (availableStocks < request.stock())
            throw new ApiException("Not enough available stock to deduct.", HttpStatus.BAD_REQUEST);

        int updatedTotalQuantity = inventory.getTotalQuantity() - request.stock();
        inventory.setTotalQuantity(updatedTotalQuantity);
        inventoryRepository.save(inventory);

        if (updatedTotalQuantity == 0)
            inventoryProducerService.sendStockStatusUpdatedEvent(new StockStatusUpdatedEvent(productId, false));

        logger.info("Deducted stock from productId={} by sellerId={} quantity={}", productId, sellerId, request.stock());
        return StockResponse.from(inventory);
    }

    @Override
    public StockResponse getStock(UUID productId) {
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ApiException("Inventory not found.", HttpStatus.NOT_FOUND));

        return StockResponse.from(inventory);
    }

    private Inventory getAuthorizedInventory(UUID productId, UUID sellerId) {
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ApiException("Inventory not found.", HttpStatus.NOT_FOUND));

        if (!inventory.getSellerId().equals(sellerId))
            throw new ApiException("Unauthorized Access.", HttpStatus.UNAUTHORIZED);

        return inventory;
    }
}
