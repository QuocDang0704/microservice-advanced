package com.mrquoc.orderservice.controller;

import java.util.function.Supplier;
import com.mrquoc.orderservice.client.InventoryClient;
import com.mrquoc.orderservice.dto.OrderDto;
import com.mrquoc.orderservice.model.Order;
import com.mrquoc.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreaker;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/order")
@Slf4j
@RequiredArgsConstructor
public class OrderController {
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final Resilience4JCircuitBreakerFactory circuitBreakerFactory;

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderDto orderDto) {
        Resilience4JCircuitBreaker circuitBreaker = circuitBreakerFactory.create("inventory");
        Supplier<Boolean> booleanSupplier = () -> orderDto.getOrderLineItems().stream()
                .allMatch(orderLineItems -> inventoryClient.checkStock(orderLineItems.getSkucode()));
        boolean productsInStock  = circuitBreaker.run(booleanSupplier, throwable -> handleErrorCase());

        if (productsInStock) {
            Order order = new Order();
            order.setOrderLineItems(orderDto.getOrderLineItems());
            order.setOrderNumber(UUID.randomUUID().toString());

            orderRepository.save(order);
            return ResponseEntity.ok("Order place Successfully");
        } else {
            return ResponseEntity.ok("Order Failed, One of the products in the order is not in stock");
        }

    }

    private Boolean handleErrorCase() {
        return false;
    }
}
