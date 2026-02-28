package com.arcanaerp.platform.products.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByCode(String code);
}
