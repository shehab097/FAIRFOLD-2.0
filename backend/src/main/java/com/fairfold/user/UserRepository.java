package com.fairfold.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByCompanyIdAndDisabledAtIsNullOrderByFirstNameAscLastNameAsc(UUID companyId);

    Optional<User> findByIdAndCompanyIdAndDisabledAtIsNull(UUID id, UUID companyId);

    long countByCompanyIdAndRoleAndDisabledAtIsNull(UUID companyId, Role role);
}
