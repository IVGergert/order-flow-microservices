package com.gergert.orderservice.entity;

public enum MenuCategory {
    PIZZA("Пицца"),
    BURGERS("Бургеры"),
    SUSHI("Суши"),
    SNACKS("Снеки"),
    DRINKS("Напитки");

    private final String title;

    MenuCategory(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
