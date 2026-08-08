package com.expense.service.service;

import com.expense.service.dto.AddExpenseResponse;
import com.expense.service.dto.CategoryBreakdownDto;
import com.expense.service.dto.ExpenseDto;
import com.expense.service.dto.ExpenseSummaryDto;
import com.expense.service.dto.MonthlyDataDto;
import com.expense.service.dto.SpendingLimitDto;
import com.expense.service.dto.SpendingLimitStatusDto;
import com.expense.service.dto.TransactionSummaryDto;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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

        Optional<SpendingLimit> limitOpt = spendingLimitRepository.findByUserId(expenseDto.getUserId());
        if(limitOpt.isEmpty() || limitOpt.get().getAmount() == null){
            return AddExpenseResponse.builder()
                    .success(true)
                    .limitExceeded(false)
                    .build();
        }

        SpendingLimit spendingLimit = limitOpt.get();
        BigDecimal amount = spendingLimit.getAmount();
        BigDecimal totalSpent = getSpendInRange(spendingLimit);
        BigDecimal remainingLimit = amount.subtract(totalSpent);
        boolean limitExceeded = totalSpent.compareTo(amount) > 0;

        return AddExpenseResponse.builder()
                .success(true)
                .limitExceeded(limitExceeded)
                .warning(limitExceeded ? String.format(
                        "Spending limit exceeded! Spent %s, limit is %s", totalSpent, amount) : null)
                .totalSpent(totalSpent)
                .remainingLimit(remainingLimit)
                .build();
    }

    public SpendingLimitStatusDto setLimit(SpendingLimitDto spendingLimitDto){
        try{
            if (spendingLimitDto.getAmount() != null && spendingLimitDto.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                spendingLimitRepository.findByUserId(spendingLimitDto.getUserId())
                        .ifPresent(spendingLimitRepository::delete);
                return SpendingLimitStatusDto.builder().success(true).build();
            }

            SpendingLimit spendingLimit = spendingLimitRepository.findByUserId(spendingLimitDto.getUserId())
                    .orElseGet(SpendingLimit::new);
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(spendingLimitDto.getDays());

            spendingLimit.setUserId(spendingLimitDto.getUserId());
            spendingLimit.setAmount(spendingLimitDto.getAmount());
            spendingLimit.setDays(spendingLimitDto.getDays());
            spendingLimit.setStartDate(startDate);
            spendingLimit.setEndDate(endDate);
            spendingLimit.setCreatedAt(Instant.now());
            spendingLimitRepository.save(spendingLimit);

            return buildStatus(spendingLimit, true);
        }catch(Exception ex){
            return SpendingLimitStatusDto.builder().success(false).build();
        }
    }

    public Optional<SpendingLimitStatusDto> getLimit(String userId){
        return spendingLimitRepository.findByUserId(userId)
                .map(spendingLimit -> buildStatus(spendingLimit, null));
    }

    private SpendingLimitStatusDto buildStatus(SpendingLimit spendingLimit, Boolean success){
        BigDecimal totalSpent = getSpendInRange(spendingLimit);
        BigDecimal remainingLimit = spendingLimit.getAmount() != null
                ? spendingLimit.getAmount().subtract(totalSpent)
                : null;
        Long daysLeft = spendingLimit.getEndDate() != null
                ? Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), spendingLimit.getEndDate()))
                : null;

        return SpendingLimitStatusDto.builder()
                .success(success)
                .amount(spendingLimit.getAmount())
                .days(spendingLimit.getDays())
                .startDate(spendingLimit.getStartDate())
                .endDate(spendingLimit.getEndDate())
                .totalSpent(totalSpent)
                .remainingLimit(remainingLimit)
                .daysLeft(daysLeft)
                .build();
    }

    private BigDecimal getSpendInRange(SpendingLimit spendingLimit){
        Timestamp rangeStart = Timestamp.valueOf(spendingLimit.getStartDate().atStartOfDay());
        // Floor at the last (re)set instant so expenses from earlier the same day,
        // before a reset, don't get counted against the new budget period.
        if (spendingLimit.getCreatedAt() != null) {
            Timestamp resetStamp = Timestamp.from(spendingLimit.getCreatedAt());
            if (resetStamp.after(rangeStart)) {
                rangeStart = resetStamp;
            }
        }
        Timestamp rangeEnd = Timestamp.valueOf(spendingLimit.getEndDate().atTime(LocalTime.MAX));

        return expenseRepository.findByUserIdAndCreatedAtBetween(spendingLimit.getUserId(), rangeStart, rangeEnd).stream()
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

    public ExpenseSummaryDto getSummary(String userId, int days){
        LocalDate startDate = LocalDate.now().minusDays(days);
        Timestamp rangeStart = Timestamp.valueOf(startDate.atStartOfDay());

        List<Expense> expenses = expenseRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(expense -> expense.getCreatedAt() != null && !expense.getCreatedAt().before(rangeStart))
                .collect(Collectors.toList());

        BigDecimal totalIncome = expenses.stream()
                .filter(expense -> "credit".equalsIgnoreCase(expense.getTransactionType()))
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = expenses.stream()
                .filter(expense -> "debit".equalsIgnoreCase(expense.getTransactionType()))
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransactionSummaryDto> recentTransactions = expenses.stream()
                .limit(10)
                .map(expense -> TransactionSummaryDto.builder()
                        .amount(expense.getAmount())
                        .merchant(expense.getMerchant())
                        .currency(expense.getCurrency())
                        .transactionType(expense.getTransactionType())
                        .createdAt(expense.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ExpenseSummaryDto.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .savings(totalIncome.subtract(totalExpense))
                .recentTransactions(recentTransactions)
                .categoryBreakdown(buildCategoryBreakdown(expenses))
                .monthlyData(buildMonthlyData(expenses, days))
                .build();
    }

    private List<CategoryBreakdownDto> buildCategoryBreakdown(List<Expense> expenses){
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            if ("credit".equalsIgnoreCase(expense.getTransactionType()) || expense.getAmount() == null) {
                continue;
            }
            categoryTotals.merge(categorize(expense.getMerchant()), expense.getAmount(), BigDecimal::add);
        }
        return categoryTotals.entrySet().stream()
                .map(entry -> CategoryBreakdownDto.builder()
                        .category(entry.getKey())
                        .totalAmount(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private List<MonthlyDataDto> buildMonthlyData(List<Expense> expenses, int days){
        int monthsToShow = Math.max(1, (int) Math.ceil(days / 30.0));
        Map<YearMonth, BigDecimal[]> monthTotals = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.now();
        for (int i = monthsToShow - 1; i >= 0; i--) {
            monthTotals.put(currentMonth.minusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Expense expense : expenses) {
            if (expense.getCreatedAt() == null || expense.getAmount() == null) {
                continue;
            }
            YearMonth expenseMonth = YearMonth.from(expense.getCreatedAt().toLocalDateTime());
            BigDecimal[] bucket = monthTotals.get(expenseMonth);
            if (bucket == null) {
                continue;
            }
            if ("credit".equalsIgnoreCase(expense.getTransactionType())) {
                bucket[0] = bucket[0].add(expense.getAmount());
            } else {
                bucket[1] = bucket[1].add(expense.getAmount());
            }
        }

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        return monthTotals.entrySet().stream()
                .map(entry -> MonthlyDataDto.builder()
                        .month(entry.getKey().format(monthFormatter))
                        .income(entry.getValue()[0])
                        .expense(entry.getValue()[1])
                        .build())
                .collect(Collectors.toList());
    }

    private String categorize(String merchant){
        if (Strings.isBlank(merchant)) {
            return "Other";
        }
        String name = merchant.toLowerCase();
        if (containsAny(name, "swiggy", "zomato", "food", "restaurant", "cafe", "dominos", "pizza", "mcdonald", "kfc", "starbucks", "eatery")) {
            return "Food";
        }
        if (containsAny(name, "amazon", "flipkart", "myntra", "ajio", "mall", "shop", "store", "meesho")) {
            return "Shopping";
        }
        if (containsAny(name, "uber", "ola", "irctc", "flight", "airlines", "indigo", "redbus", "travel", "metro", "fuel", "petrol")) {
            return "Travel";
        }
        if (containsAny(name, "electricity", "recharge", "bill", "dth", "broadband", "gas", "water", "jio", "Airtel")) {
            return "Bills";
        }
        if (containsAny(name, "netflix", "hotstar", "prime", "spotify", "bookmyshow", "movie", "cinema")) {
            return "Entertainment";
        }
        if (containsAny(name, "bigbasket", "grofers", "dmart", "grocery", "supermarket", "blinkit", "zepto")) {
            return "Groceries";
        }
        return "Other";
    }

    private boolean containsAny(String text, String... keywords){
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void setCurrency(ExpenseDto expenseDto){
        if(Objects.isNull(expenseDto.getCurrency())){
            expenseDto.setCurrency("inr");
        }
    }


}



