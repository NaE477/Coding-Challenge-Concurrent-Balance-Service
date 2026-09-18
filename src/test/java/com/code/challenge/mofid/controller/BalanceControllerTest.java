package com.code.challenge.mofid.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BalanceControllerTest {

    @Autowired
    private MockMvc mvc;

    private String prefix;
    private String a;
    private String b;

    @BeforeEach
    void uniqueIds() {
        prefix = UUID.randomUUID().toString().substring(0, 8);
        a = prefix + "-A";
        b = prefix + "-B";
    }

    @Test
    void openingAnAccountReturnsCreatedWithItsLocation() throws Exception {
        open(a, 1_000)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/accounts/" + a + "/balance"))
                .andExpect(jsonPath("$.accountId").value(a))
                .andExpect(jsonPath("$.balance").value(1_000));
    }

    @Test
    void openingTheSameAccountTwiceIsAConflict() throws Exception {
        open(a, 1_000);

        open(a, 1_000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DuplicateAccountException"));
    }

    @Test
    void unknownAccountIsNotFound() throws Exception {
        mvc.perform(get("/accounts/{id}/balance", a))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("AccountNotFoundException"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void creditDebitAndTransferAreAppliedAndVisibleInTheBalance() throws Exception {
        open(a, 1_000);
        open(b, 500);

        postJson("/accounts/" + a + "/credits", amount(500, tx(1))).andExpect(status().isNoContent());
        postJson("/accounts/" + a + "/debits", amount(700, tx(2))).andExpect(status().isNoContent());
        postJson("/transfers", transfer(a, b, 300, tx(3))).andExpect(status().isNoContent());

        expectBalance(a, 500);
        expectBalance(b, 800);
    }

    @Test
    void debitBeyondTheBalanceIsUnprocessableAndChangesNothing() throws Exception {
        open(a, 1_000);

        postJson("/accounts/" + a + "/debits", amount(1_200, tx(1)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("InsufficientFundsException"));

        expectBalance(a, 1_000);
    }

    @Test
    void replayedRequestSucceedsEachTimeButIsAppliedOnce() throws Exception {
        open(a, 1_000);

        for (int i = 0; i < 3; i++) {
            postJson("/accounts/" + a + "/credits", amount(100, tx(1))).andExpect(status().isNoContent());
        }

        expectBalance(a, 1_100);
    }

    @Test
    void reusingATransactionIdForADifferentRequestIsAConflict() throws Exception {
        open(a, 1_000);
        postJson("/accounts/" + a + "/credits", amount(100, tx(1)));

        postJson("/accounts/" + a + "/debits", amount(100, tx(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IdempotencyConflictException"));

        expectBalance(a, 1_100);
    }

    @Test
    void invalidInputIsABadRequest() throws Exception {
        open(a, 1_000);
        open(b, 500);

        postJson("/accounts/" + a + "/credits", amount(0, tx(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidAmountException"));
        postJson("/accounts/" + a + "/credits", "{\"amount\": 100}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidRequestException"));
        postJson("/transfers", transfer(a, a, 100, tx(2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SameAccountTransferException"));

        expectBalance(a, 1_000);
    }

    @Test
    void deletedAccountIsGoneForGoodAndItsIdCantBeReused() throws Exception {
        open(a, 1_000);

        mvc.perform(delete("/accounts/{id}", a)).andExpect(status().isNoContent());

        mvc.perform(get("/accounts/{id}/balance", a)).andExpect(status().isNotFound());
        open(a, 0)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DeletedAccountException"));
        mvc.perform(delete("/accounts/{id}", a)).andExpect(status().isNotFound());
        mvc.perform(get("/reports/accounts"))
                .andExpect(jsonPath("$.accounts[?(@.accountId == '%s')]".formatted(a)).isEmpty());
    }

    @Test
    void malformedJsonIsABadRequestProblem() throws Exception {
        postJson("/accounts/" + a + "/credits", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private ResultActions open(String accountId, long initialBalance) throws Exception {
        return postJson("/accounts", "{\"accountId\": \"%s\", \"initialBalance\": %d}".formatted(accountId, initialBalance));
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void expectBalance(String accountId, long expected) throws Exception {
        mvc.perform(get("/accounts/{id}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.balance").value(expected));
    }

    private String tx(int number) {
        return prefix + "-TX-" + number;
    }

    private static String amount(long amount, String transactionId) {
        return "{\"amount\": %d, \"transactionId\": \"%s\"}".formatted(amount, transactionId);
    }

    private static String transfer(String source, String destination, long amount, String transactionId) {
        return "{\"sourceAccountId\": \"%s\", \"destinationAccountId\": \"%s\", \"amount\": %d, \"transactionId\": \"%s\"}"
                .formatted(source, destination, amount, transactionId);
    }
}
