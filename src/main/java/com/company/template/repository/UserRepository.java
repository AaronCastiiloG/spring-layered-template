package com.company.template.repository;

import com.company.template.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

	@Query("SELECT u FROM User u LEFT JOIN FETCH u.logs WHERE u.id = :id")
	Optional<User> findByIdWithLogs(UUID id);

	boolean existsByName(String name);

	boolean existsByEmail(String email);

	boolean existsByNameAndIdNot(String name, UUID id);

	boolean existsByEmailAndIdNot(String email, UUID id);

}
