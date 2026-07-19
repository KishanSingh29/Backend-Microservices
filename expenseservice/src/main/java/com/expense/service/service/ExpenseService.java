package com.expense.service.service;

import com.expense.service.dto.AddExpenseResponse;
import com.expense.service.dto.ExpenseDto;
import com.expense.service.dto.SpendingLimitDto;
import com.expense.service.dto.SpendingLimitStatusDto;
import com.expense.service.entities.Expense;
import com.expense.service.entities.SpendingLimit;
import com.expense.service.repository.ExpenseRepository;
import com.expense.service.repository.SpendingLimitRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ExpenseService
{

    private final ExpenseRepository expenseRepository;

    private final SpendingLimitRepository spendingLimitRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ExpenseService(ExpenseRepository expenseRepository, SpendingLimitRepository spendingLimitRepository){
        this.expenseRepository = expenseRepository;
        this.spendingLimitRepository = spendingLimitRepository;
    }

    public AddExpenseResponse createExpense(ExpenseDto expenseDto){
        setCurrency(expenseDto);
        try{
            expenseRepository.save(objectMapper.convertValue(expenseDto, Expense.class));
        }catch(Exception ex){
            return AddExpenseResponse.builder().success(false).build();
        }

        BigDecimal monthlySpend = getMonthlySpend(expenseDto.getUserId());
        Optional<SpendingLimit> limitOpt = spendingLimitRepository.findByUserId(expenseDto.getUserId());
        if(limitOpt.isEmpty() || limitOpt.get().getMonthlyLimit() == null){
            return AddExpenseResponse.builder()
                    .success(true)
                    .limitExceeded(false)
                    .totalSpent(monthlySpend)
                    .build();
        }

        BigDecimal monthlyLimit = limitOpt.get().getMonthlyLimit();
        BigDecimal remainingLimit = monthlyLimit.subtract(monthlySpend);
        boolean limitExceeded = monthlySpend.compareTo(monthlyLimit) > 0;

        return AddExpenseResponse.builder()
                .success(true)
                .limitExceeded(limitExceeded)
                .warning(limitExceeded ? String.format(
                        "Monthly spending limit exceeded! Spent %s, limit is %s", monthlySpend, monthlyLimit) : null)
                .totalSpent(monthlySpend)
                .remainingLimit(remainingLimit)
                .build();
    }

    public SpendingLimitStatusDto setLimit(SpendingLimitDto spendingLimitDto){
        try{
            SpendingLimit spendingLimit = spendingLimitRepository.findByUserId(spendingLimitDto.getUserId())
                    .orElseGet(SpendingLimit::new);
            spendingLimit.setUserId(spendingLimitDto.getUserId());
            spendingLimit.setMonthlyLimit(spendingLimitDto.getMonthlyLimit());
            spendingLimitRepository.save(spendingLimit);

            BigDecimal monthlySpend = getMonthlySpend(spendingLimitDto.getUserId());
            BigDecimal remainingLimit = spendingLimit.getMonthlyLimit() != null
                    ? spendingLimit.getMonthlyLimit().subtract(monthlySpend)
                    : null;

            return SpendingLimitStatusDto.builder()
                    .success(true)
                    .monthlyLimit(spendingLimit.getMonthlyLimit())
                    .totalSpent(monthlySpend)
                    .remainingLimit(remainingLimit)
                    .build();
        }catch(Exception ex){
            return SpendingLimitStatusDto.builder().success(false).build();
        }
    }

    public Optional<SpendingLimitStatusDto> getLimit(String userId){
        return spendingLimitRepository.findByUserId(userId)
                .map(limit -> {
                    BigDecimal monthlySpend = getMonthlySpend(userId);
                    BigDecimal remainingLimit = limit.getMonthlyLimit() != null
                            ? limit.getMonthlyLimit().subtract(monthlySpend)
                            : null;
                    return SpendingLimitStatusDto.builder()
                            .monthlyLimit(limit.getMonthlyLimit())
                            .totalSpent(monthlySpend)
                            .remainingLimit(remainingLimit)
                            .build();
                });
    }

    private BigDecimal getMonthlySpend(String userId){
        LocalDate today = LocalDate.now();
        Timestamp monthStart = Timestamp.valueOf(today.withDayOfMonth(1).atStartOfDay());
        Timestamp monthEnd = Timestamp.valueOf(today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX));

        return expenseRepository.findByUserIdAndCreatedAtBetween(userId, monthStart, monthEnd).stream()
                .filter(expense -> !"credit".equalsIgnoreCase(expense.getTransactionType()))
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean updateExpense(ExpenseDto expenseDto){
        setCurrency(expenseDto);
        Optional<Expense> expenseFoundOpt = expenseRepository.findByUserIdAndExternalId(expenseDto.getUserId(), expenseDto.getExternalId());
        if(expenseFoundOpt.isEmpty()){
            return false;
        }
        Expense expense = expenseFoundOpt.get();
        expense.setAmount(expenseDto.getAmount());
        expense.setMerchant(Strings.isNotBlank(expenseDto.getMerchant())?expenseDto.getMerchant():expense.getMerchant());
        expense.setCurrency(Strings.isNotBlank(expenseDto.getCurrency())?expenseDto.getMerchant():expense.getCurrency());
        expense.setTransactionType(Strings.isNotBlank(expenseDto.getTransactionType())?expenseDto.getTransactionType():expense.getTransactionType());
        expenseRepository.save(expense);
        return true;
    }

    public List<ExpenseDto> getExpenses(String userId){
        List<Expense> expenseOpt = expenseRepository.findByUserId(userId);
        return objectMapper.convertValue(expenseOpt, new TypeReference<List<ExpenseDto>>() {});
    }

    private void setCurrency(ExpenseDto expenseDto){
        if(Objects.isNull(expenseDto.getCurrency())){
            expenseDto.setCurrency("inr");
        }
    }


}



