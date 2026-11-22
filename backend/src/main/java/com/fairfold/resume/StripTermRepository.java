package com.fairfold.resume;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StripTermRepository extends JpaRepository<StripTerm, UUID> {

    List<StripTerm> findByKind(String kind);
}
