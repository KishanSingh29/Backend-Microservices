package com.expense.service.controller;

import com.expense.service.dto.AddExpenseResponse;
import com.expense.service.dto.ExpenseDto;
import com.expense.service.dto.ExpenseSummaryDto;
import com.expense.service.dto.SpendingLimitDto;
import com.expense.service.dto.SpendingLimitStatusDto;
import com.expense.service.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Expense Management", description = "Add expenses, budget tracking, analytics")
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
            return ResponseEntity.ok(expenseDtoList);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "Add Expense/Income",
            description = """
            Add a new transaction.
            transaction_type: 'debit' for expense, 'credit' for income.
            If spending limit is set, checks budget and returns warning if exceeded.
            Header required: X-User-Id (UUID from login)
            """)
    @Parameter(name = "X-User-Id", in = ParameterIn.HEADER,
            required = true,
            example = "f72b9ce8-d7a4-4ca6-b4d7-5eae1cf197ac")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(example = """
                {
                  "amount": 500,
                  "merchant": "Zomato",
                  "currency": "inr",
                  "transaction_type": "debit"
                }
              """)))
    @ApiResponse(responseCode = "200", description = "Expense added",
            content = @Content(schema = @Schema(example = """
                {
                  "success": true,
                  "limitExceeded": false,
                  "warning": null,
                  "totalSpent": 500,
                  "remainingLimit": 4500
                }
              """)))
    @SecurityRequirement(name = "Bearer Auth")
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

    @Operation(summary = "Set Spending Limit / Budget",
            description = """
            Set monthly spending budget.
            amount=0, days=0 → Resets/deletes budget.
            Uses createdAt timestamp - only expenses AFTER setting limit are counted.
            """)
    @Parameter(name = "X-User-Id", in = ParameterIn.HEADER,
            required = true,
            example = "f72b9ce8-d7a4-4ca6-b4d7-5eae1cf197ac")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(example = """
                {
                  "amount": 5000,
                  "days": 30
                }
              """)))
    @ApiResponse(responseCode = "200", description = "Budget set successfully",
            content = @Content(schema = @Schema(example = """
                {
                  "success": true,
                  "amount": 5000,
                  "days": 30,
                  "startDate": "2026-08-17",
                  "endDate": "2026-09-16",
                  "totalSpent": 0,
                  "remainingLimit": 5000,
                  "daysLeft": 30
                }
              """)))
    @SecurityRequirement(name = "Bearer Auth")
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

    @Operation(summary = "Get Current Budget Status",
            description = "Get spending limit details - remaining budget, total spent, days left.")
    @Parameter(name = "X-User-Id", in = ParameterIn.HEADER,
            required = true,
            example = "f72b9ce8-d7a4-4ca6-b4d7-5eae1cf197ac")
    @ApiResponse(responseCode = "200", description = "Current budget status",
            content = @Content(schema = @Schema(example = """
                {
                  "amount": 5000,
                  "days": 30,
                  "startDate": "2026-08-17",
                  "endDate": "2026-09-16",
                  "totalSpent": 1500,
                  "remainingLimit": 3500,
                  "daysLeft": 28
                }
              """)))
    @SecurityRequirement(name = "Bearer Auth")
    @GetMapping("/getLimit")
    public ResponseEntity<SpendingLimitStatusDto> getLimit(
            @RequestHeader(value = "X-User-Id") @NonNull String userId) {
        return expenseService.getLimit(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(summary = "Get Expense Analytics",
            description = """
            Get complete financial summary.
            days parameter filters data: 7=last week, 30=last month, 90=3 months, 180=6 months (default).
            Returns: income, expense, savings, charts data, category breakdown, recent transactions.
            """)
    @Parameter(name = "X-User-Id", in = ParameterIn.HEADER,
            required = true,
            example = "f72b9ce8-d7a4-4ca6-b4d7-5eae1cf197ac")
    @Parameter(name = "days", in = ParameterIn.QUERY,
            example = "30",
            description = "Filter period: 7, 30, 90, 180")
    @ApiResponse(responseCode = "200", description = "Financial summary",
            content = @Content(schema = @Schema(example = """
                {
                  "totalIncome": 50000,
                  "totalExpense": 25000,
                  "savings": 25000,
                  "recentTransactions": [
                    {
                      "amount": 500,
                      "merchant": "Zomato",
                      "currency": "INR",
                      "transactionType": "debit",
                      "createdAt": "2026-08-17T10:00:00Z"
                    }
                  ],
                  "categoryBreakdown": [
                    {"category": "Food", "totalAmount": 5000},
                    {"category": "Shopping", "totalAmount": 3000}
                  ],
                  "monthlyData": [
                    {"month": "Aug 2026", "income": 50000, "expense": 25000}
                  ]
                }
              """)))
    @SecurityRequirement(name = "Bearer Auth")
    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummaryDto> getSummary(
            @RequestHeader(value = "X-User-Id") @NonNull String userId,
            @RequestParam(defaultValue = "180") int days) {
        try {
            return ResponseEntity.ok(expenseService.getSummary(userId, days));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkHealth() {
        return ResponseEntity.ok(true);
    }
}