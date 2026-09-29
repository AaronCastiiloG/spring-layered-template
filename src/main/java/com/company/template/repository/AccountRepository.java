package com.company.template.repository;

import com.company.template.entity.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

	@EntityGraph(attributePaths = "roles")
	Optional<Account> findByUsername(String username);

}
