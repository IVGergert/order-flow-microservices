package com.gergert.orderservice.dto;

import com.gergert.orderservice.entity.MenuItem;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)

public interface MenuMapper {

    MenuItem toEntity(MenuItemDto menuItemDto);

    @Mapping(target = "categoryTitle", expression = "java(menuItem.getCategory().getTitle())")
    MenuItemDto toMenuDto(MenuItem menuItem);
}
