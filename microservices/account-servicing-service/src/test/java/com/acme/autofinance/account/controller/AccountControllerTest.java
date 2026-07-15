package com.acme.autofinance.account.controller;

import com.acme.autofinance.account.domain.Account;
import com.acme.autofinance.account.domain.AccountStatus;
import com.acme.autofinance.account.service.AccountNotFoundException;
import com.acme.autofinance.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(AccountExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    private Account sample(Long id, String number, Long loanId) {
        Account a = new Account();
        a.setId(id);
        a.setAccountNumber(number);
        a.setLoanId(loanId);
        a.setCustomerName("Jordan Rivera");
        a.setCurrentBalance(new BigDecimal("18500.00"));
        a.setStatus(AccountStatus.CURRENT);
        return a;
    }

    @Test
    void getAccountReturnsAccount() throws Exception {
        when(accountService.getAccountDetails(eq(1L))).thenReturn(sample(1L, "ACCT-0001", 1001L));

        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountNumber").value("ACCT-0001"))
                .andExpect(jsonPath("$.loanId").value(1001));
    }

    @Test
    void getAccountReturns404WhenMissing() throws Exception {
        when(accountService.getAccountDetails(eq(99L)))
                .thenThrow(new AccountNotFoundException("Account not found: 99"));

        mockMvc.perform(get("/api/accounts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Account not found: 99"));
    }

    @Test
    void getByNumberReturnsAccount() throws Exception {
        when(accountService.getByAccountNumber(eq("ACCT-0001"))).thenReturn(sample(1L, "ACCT-0001", 1001L));

        mockMvc.perform(get("/api/accounts/number/ACCT-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACCT-0001"));
    }

    @Test
    void updateAddressReturnsUpdatedAccount() throws Exception {
        Account updated = sample(1L, "ACCT-0001", 1001L);
        updated.setMailingAddress("500 New Rd, Ann Arbor MI");
        when(accountService.updateAddress(eq(1L), eq("500 New Rd, Ann Arbor MI"))).thenReturn(updated);

        mockMvc.perform(put("/api/accounts/1/address").param("address", "500 New Rd, Ann Arbor MI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mailingAddress").value("500 New Rd, Ann Arbor MI"));
    }

    @Test
    void getPayoffQuoteReturnsQuote() throws Exception {
        Map<String, Object> quote = new HashMap<>();
        quote.put("accountNumber", "ACCT-0001");
        quote.put("currentBalance", new BigDecimal("18500.00"));
        quote.put("perDiemInterest", new BigDecimal("3.04"));
        quote.put("tenDayInterest", new BigDecimal("30.40"));
        quote.put("outstandingFees", new BigDecimal("0.00"));
        quote.put("totalPayoffAmount", new BigDecimal("18530.40"));
        when(accountService.calculatePayoffQuote(eq(1L))).thenReturn(quote);

        mockMvc.perform(get("/api/accounts/1/payoff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACCT-0001"))
                .andExpect(jsonPath("$.totalPayoffAmount").value(18530.40));
    }

    @Test
    void processEarlyTerminationReturnsResult() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("accountNumber", "ACCT-0001");
        result.put("finalPayoffAmount", new BigDecimal("18530.40"));
        result.put("status", "TERMINATED");
        when(accountService.processEarlyTermination(eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/accounts/1/early-termination"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.finalPayoffAmount").value(18530.40));
    }

    @Test
    void getDelinquentAccountsReturnsList() throws Exception {
        when(accountService.getDelinquentAccounts(eq(30)))
                .thenReturn(Arrays.asList(sample(2L, "ACCT-0002", 1002L), sample(3L, "ACCT-0003", 1003L)));

        mockMvc.perform(get("/api/accounts/delinquent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountNumber").value("ACCT-0002"));
    }
}
