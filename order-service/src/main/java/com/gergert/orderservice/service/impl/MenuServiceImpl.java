package com.gergert.orderservice.service.impl;

import com.gergert.orderservice.entity.MenuItem;
import com.gergert.orderservice.repository.MenuItemRepository;
import com.gergert.orderservice.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {
    private final MenuItemRepository menuItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MenuItem> getAllItems() {
        return menuItemRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItem getItemById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Menu item with id `%s` not found".formatted(id)
                        )
                );
    }
}
