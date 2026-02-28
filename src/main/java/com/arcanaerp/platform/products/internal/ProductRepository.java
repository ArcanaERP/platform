package com.arcanaerp.platform.products.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    List<Product> findAllByOrderByCreatedAtDesc();

    Page<Product> findByActive(boolean active, Pageable pageable);
}
