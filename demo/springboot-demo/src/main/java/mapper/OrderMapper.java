package com.example.springbootdemo.mapper;

import com.example.springbootdemo.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper {

    int insert(Order order);

    Order selectById(@Param("id") Long id);

    List<Order> selectAll();

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int selectCountByProductId(@Param("productId") Long productId);
}
