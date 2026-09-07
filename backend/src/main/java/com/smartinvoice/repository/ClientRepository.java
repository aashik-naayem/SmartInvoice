package com.smartinvoice.repository;

import com.smartinvoice.entity.Client;
import com.smartinvoice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByUser(User user);

    Optional<Client> findByIdAndUser(Long id, User user);

    long countByUser(User user);
}
