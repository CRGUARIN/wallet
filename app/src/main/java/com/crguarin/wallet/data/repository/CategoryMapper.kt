package com.crguarin.wallet.data.repository

import com.crguarin.wallet.data.db.CategoryEntity
import com.crguarin.wallet.domain.model.Category

fun CategoryEntity.toDomain(): Category =
    Category(
        id = id,
        name = name,
        systemKey = systemKey,
        type = type,
        parentId = parentId,
        color = color,
        icon = icon,
        isArchived = isArchived,
        sortOrder = sortOrder,
    )

fun Category.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        systemKey = systemKey,
        type = type,
        parentId = parentId,
        color = color,
        icon = icon,
        isArchived = isArchived,
        sortOrder = sortOrder,
    )
