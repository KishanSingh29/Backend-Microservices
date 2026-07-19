package com.expense.service.repository;

import com.expense.service.entities.SpendingLimit;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface SpendingLimitRepository extends CrudRepository<SpendingLimit, Long> {

    Optional<SpendingLimit> findByUserId(String userId);

}
