package com.gergert.orderservice.service.impl;

import com.gergert.orderservice.entity.MenuCategory;
import com.gergert.orderservice.entity.MenuItem;
import com.gergert.orderservice.repository.MenuItemRepository;
import com.gergert.orderservice.exception.MenuItemNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {
    @Mock private MenuItemRepository repository;
    @InjectMocks private MenuServiceImpl service;

    @Test
    void getAllItems_shouldReturnItemsFromRepository() {
        MenuItem item = new MenuItem(1L, "Pizza", new BigDecimal("10.00"), "desc", "image", MenuCategory.PIZZA);
        when(repository.findAll()).thenReturn(List.of(item));

        assertThat(service.getAllItems()).containsExactly(item);
        verify(repository).findAll();
    }

    @Test
    void getItemById_shouldReturnItem() {
        MenuItem item = new MenuItem(1L, "Pizza", new BigDecimal("10.00"), "desc", "image", MenuCategory.PIZZA);
        when(repository.findById(1L)).thenReturn(Optional.of(item));

        assertThat(service.getItemById(1L)).isSameAs(item);
    }

    @Test
    void getItemById_shouldReturnNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getItemById(99L))
                .isInstanceOf(MenuItemNotFoundException.class);
    }
}
