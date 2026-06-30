package com.example.springbootdemo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class OrderCreateRequest {

    @NotNull(message = "productId不能为空")
    private Long productId;

    @NotNull(message = "quantity不能为空")
    @Min(value = 1, message = "quantity必须大于0")
    private Integer quantity;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
