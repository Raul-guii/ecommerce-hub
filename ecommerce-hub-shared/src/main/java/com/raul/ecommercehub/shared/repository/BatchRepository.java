package com.raul.ecommercehub.shared.repository;

import com.raul.ecommercehub.shared.domain.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID> {
}