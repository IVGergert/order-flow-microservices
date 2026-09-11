package com.gergert.orderservice.service.impl;

import com.gergert.orderservice.entity.MenuCategory;
import com.gergert.orderservice.entity.MenuItem;
import com.gergert.orderservice.repository.MenuItemRepository;
import com.gergert.orderservice.exception.MenuItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private MenuServiceImpl service;

    private MenuItem pizza;

    @BeforeEach
    void setUp() {
        pizza = new MenuItem(
                1L,
                "Pizza",
                new BigDecimal("10.00"),
                "Pizza description",
                "pizza.jpg",
                MenuCategory.PIZZA
        );
    }


    // getAllItems()

    @Test
    void getAllItems_shouldReturnAllMenuItems() {
        MenuItem burger = new MenuItem(
                2L,
                "Burger",
                new BigDecimal("8.50"),
                "Burger description",
                "burger.jpg",
                MenuCategory.BURGERS
        );

        when(menuItemRepository.findAll())
                .thenReturn(List.of(pizza, burger));

        List<MenuItem> result = service.getAllItems();

        assertThat(result)
                .containsExactly(pizza, burger);

        verify(menuItemRepository)
                .findAll();

        verifyNoMoreInteractions(menuItemRepository);
    }

    @Test
    void getAllItems_shouldReturnEmptyListWhenMenuIsEmpty() {
        when(menuItemRepository.findAll())
                .thenReturn(List.of());

        List<MenuItem> result = service.getAllItems();

        assertThat(result)
                .isEmpty();

        verify(menuItemRepository)
                .findAll();

        verifyNoMoreInteractions(menuItemRepository);
    }

    // getItemById()

    @Test
    void getItemById_shouldReturnItemWhenExists() {
        when(menuItemRepository.findById(1L))
                .thenReturn(Optional.of(pizza));

        MenuItem result = service.getItemById(1L);

        assertThat(result)
                .isSameAs(pizza);

        verify(menuItemRepository)
                .findById(1L);

        verifyNoMoreInteractions(menuItemRepository);
    }

    @Test
    void getItemById_shouldThrowWhenItemDoesNotExist() {
        when(menuItemRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getItemById(99L))
                .isInstanceOf(MenuItemNotFoundException.class)
                .hasMessage("Menu item with id `99` not found");

        verify(menuItemRepository)
                .findById(99L);

        verifyNoMoreInteractions(menuItemRepository);
    }
}
