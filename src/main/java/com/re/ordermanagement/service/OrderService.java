package com.re.ordermanagement.service;

import com.re.ordermanagement.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    List<Order> findAll();

    List<Order> search(String keyword);

    Optional<Order> findById(long id);

    Order create(Order order);

    Order update(long id, Order order);

    void delete(long id);

    boolean isOrderCodeUnique(String orderCode, Long ignoreId);
}

