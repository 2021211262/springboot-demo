package com.example.springbootdemo.controller;

import com.example.springbootdemo.common.Result;
import com.example.springbootdemo.dto.OrderCreateRequest;
import com.example.springbootdemo.entity.Order;
import com.example.springbootdemo.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public Result<Order> createOrder(@RequestBody @Valid OrderCreateRequest request) {
        Order order = orderService.createOrder(request.getProductId(), request.getQuantity());
        return Result.success(order);
    }

    @GetMapping("/{id}")
    public Result<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return Result.success(order);
    }

    @GetMapping
    public Result<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return Result.success(orders);
    }
}
