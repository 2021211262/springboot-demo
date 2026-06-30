package com.example.springbootdemo.mapper;

import com.example.springbootdemo.entity.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper {

    int insert(Product product);

    Product selectById(@Param("id") Long id);

    List<Product> selectAll();

    int deleteById(@Param("id") Long id);

    int updateStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    Product selectByName(@Param("name") String name);
}
