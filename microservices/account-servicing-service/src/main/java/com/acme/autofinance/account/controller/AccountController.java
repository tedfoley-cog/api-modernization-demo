package com.acme.autofinance.account.controller;

import com.acme.autofinance.account.domain.Account;
import com.acme.autofinance.account.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API for the account-servicing bounded context. Endpoint paths and
 * response shapes are preserved from the legacy monolith controller.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountDetails(id));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<Account> getByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getByAccountNumber(accountNumber));
    }

    @PutMapping("/{id}/address")
    public ResponseEntity<Account> updateAddress(
            @PathVariable Long id,
            @RequestParam String address) {
        return ResponseEntity.ok(accountService.updateAddress(id, address));
    }

    @GetMapping("/{id}/payoff")
    public ResponseEntity<Map<String, Object>> getPayoffQuote(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.calculatePayoffQuote(id));
    }

    @PostMapping("/{id}/early-termination")
    public ResponseEntity<Map<String, Object>> processEarlyTermination(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.processEarlyTermination(id));
    }

    @GetMapping("/delinquent")
    public ResponseEntity<List<Account>> getDelinquentAccounts(
            @RequestParam(defaultValue = "30") int daysPastDue) {
        return ResponseEntity.ok(accountService.getDelinquentAccounts(daysPastDue));
    }
}
