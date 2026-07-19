package com.expense.service.controller;

import com.expense.service.dto.AddExpenseResponse;
import com.expense.service.dto.ExpenseDto;
import com.expense.service.dto.SpendingLimitDto;
import com.expense.service.dto.SpendingLimitStatusDto;
import com.expense.service.service.ExpenseService;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; // ✅ sirf yeh import rakho
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expense/v1")
public class ExpenseController {

    private final ExpenseService expenseService;

    @Autowired
    ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/getExpense")
    public ResponseEntity<List<ExpenseDto>> getExpense(
            @RequestHeader(value = "X-User-Id") @NonNull String userId) {
        try {
            List<ExpenseDto> expenseDtoList = expenseService.getExpenses(userId);
            return ResponseEntity.ok(expenseDtoList); // ✅ clean way
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/addExpense")
    public ResponseEntity<AddExpenseResponse> addExpenses(
            @RequestHeader(value = "X-User-Id") @NonNull String userId,
            @RequestBody ExpenseDto expenseDto) {
        try {
            expenseDto.setUserId(userId);
            return ResponseEntity.ok(expenseService.createExpense(expenseDto));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AddExpenseResponse.builder().success(false).build());
        }
    }

    @PostMapping("/setLimit")
    public ResponseEntity<SpendingLimitStatusDto> setLimit(
            @RequestHeader(value = "X-User-Id") @NonNull String userId,
            @RequestBody SpendingLimitDto spendingLimitDto) {
        try {
            spendingLimitDto.setUserId(userId);
            return ResponseEntity.ok(expenseService.setLimit(spendingLimitDto));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(SpendingLimitStatusDto.builder().success(false).build());
        }
    }

    @GetMapping("/getLimit")
    public ResponseEntity<SpendingLimitStatusDto> getLimit(
            @RequestHeader(value = "X-User-Id") @NonNull String userId) {
        return expenseService.getLimit(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkHealth() {
        return ResponseEntity.ok(true);
    }
}