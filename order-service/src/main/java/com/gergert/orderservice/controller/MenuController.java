package com.gergert.orderservice.controller;

import com.gergert.orderservice.dto.MenuItemDto;
import com.gergert.orderservice.dto.MenuMapper;
import com.gergert.orderservice.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {
    private final MenuService menuService;
    private final MenuMapper menuMapper;

    @GetMapping
    public List<MenuItemDto> getAll() {

        return menuService.getAllItems()
                .stream()
                .map(menuMapper::toMenuDto)
                .toList();
    }

    @GetMapping("/{id}")
    public MenuItemDto getById(@PathVariable Long id) {

        var menuItem = menuService.getItemById(id);

        return menuMapper.toMenuDto(menuItem);
    }

}
