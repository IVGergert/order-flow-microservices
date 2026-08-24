package com.gergert.orderservice.service;

import com.gergert.orderservice.entity.MenuItem;

import java.util.List;

public interface MenuService {
    List<MenuItem> getAllItems();
    MenuItem getItemById(Long id);
}
