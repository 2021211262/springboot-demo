package com.example.springbootdemo.service;

import com.example.springbootdemo.common.BusinessException;
import com.example.springbootdemo.common.ResultCode;
import com.example.springbootdemo.entity.Order;
import com.example.springbootdemo.entity.OrderStatus;
import com.example.springbootdemo.entity.Product;
import com.example.springbootdemo.mapper.OrderMapper;
import com.example.springbootdemo.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderCreateService {

    private static final Logger log = LoggerFactory.getLogger(OrderCreateService.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductService productService;

    @Transactional(rollbackFor = Exception.class)
    public Order doCreateOrder(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "参数无效：productId和quantity必填，quantity必须大于0");
        }
        if (quantity > 9999) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "单次购买数量不能超过9999");
        }

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "商品不存在");
        }

        if (product.getPrice() == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "商品价格数据异常");
        }

        if (product.getStock() < quantity) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "库存不足");
        }

        int updated = productMapper.updateStock(productId, quantity);
        if (updated == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "库存不足");
        }

        productService.invalidateProductCache(productId);

        Order order = new Order();
        order.setProductId(productId);
        order.setProductName(product.getName());
        order.setQuantity(quantity);
        order.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(OrderStatus.PENDING.getCode());
        orderMapper.insert(order);

        log.info("Order created: id={}, productId={}, quantity={}", order.getId(), productId, quantity);
        return order;
    }
}
