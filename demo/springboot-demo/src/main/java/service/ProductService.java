package com.example.springbootdemo.service;

import com.example.springbootdemo.common.BusinessException;
import com.example.springbootdemo.common.CacheConstants;
import com.example.springbootdemo.common.ResultCode;
import com.example.springbootdemo.entity.Product;
import com.example.springbootdemo.mapper.OrderMapper;
import com.example.springbootdemo.mapper.ProductMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private static final long CACHE_EXPIRE_MINUTES = 30;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public Product createProduct(Product product) {

        Product existing = productMapper.selectByName(product.getName());
        if (existing != null) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "商品名称已存在");
        }
        productMapper.insert(product);

        try {
            String json = objectMapper.writeValueAsString(product);
            redisTemplate.opsForValue().set(CacheConstants.PRODUCT_CACHE_PREFIX + product.getId(), json, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache product after creation: id={}", product.getId(), e);
        }

        return product;
    }

    public Product getProductById(Long id) {
        String cacheKey = CacheConstants.PRODUCT_CACHE_PREFIX + id;
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            try {
                log.debug("Cache hit for product id={}", id);
                return objectMapper.readValue(cachedValue, Product.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize cached product id={}", id, e);
                redisTemplate.delete(cacheKey);
            }
        }

        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "商品不存在");
        }

        try {
            String json = objectMapper.writeValueAsString(product);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("Cache written for product id={}", id);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize product id={}", id, e);
        }

        return product;
    }

    public List<Product> getAllProducts() {
        return productMapper.selectAll();
    }

    public void invalidateProductCache(Long productId) {
        redisTemplate.delete(CacheConstants.PRODUCT_CACHE_PREFIX + productId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "商品不存在");
        }

        int orderCount = orderMapper.selectCountByProductId(id);
        if (orderCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "该商品存在关联订单，无法删除");
        }

        productMapper.deleteById(id);

        String cacheKey = CacheConstants.PRODUCT_CACHE_PREFIX + id;
        redisTemplate.delete(cacheKey);
    }
}
