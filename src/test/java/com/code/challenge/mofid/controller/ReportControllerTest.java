package com.code.challenge.mofid.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The context is shared across tests, so every test works on its own ids and only asserts on those.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mvc;

    private String prefix;

    @BeforeEach
    void uniquePrefix() {
        prefix = UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void accountsReportListsBalancesAndATotalThatMatchesThem() throws Exception {
        open(id("A"), 1_000);
        open(id("B"), 500);
        postJson("/transfers", """
                {"sourceAccountId": "%s", "destinationAccountId": "%s", "amount": 300, "transactionId": "%s"}"""
                .formatted(id("A"), id("B"), id("TX-1"))).andExpect(status().isNoContent());

        String body = mvc.perform(get("/reports/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[?(@.accountId == '%s')].balance".formatted(id("A"))).value(contains(700)))
                .andExpect(jsonPath("$.accounts[?(@.accountId == '%s')].balance".formatted(id("B"))).value(contains(800)))
                .andReturn().getResponse().getContentAsString();

        List<Number> balances = JsonPath.read(body, "$.accounts[*].balance");
        BigInteger sum = balances.stream()
                .map(balance -> BigInteger.valueOf(balance.longValue()))
                .reduce(BigInteger.ZERO, BigInteger::add);
        assertThat(new BigInteger(JsonPath.read(body, "$.totalBalance").toString())).isEqualTo(sum);
        assertThat((Integer) JsonPath.read(body, "$.accountCount")).isEqualTo(balances.size());
    }

    @Test
    void transactionsReportListsEachAppliedTransactionOnceAndNoFailedOnes() throws Exception {
        open(id("A"), 1_000);
        for (int i = 0; i < 3; i++) {
            postJson("/accounts/" + id("A") + "/credits", amount(100, id("TX-1"))).andExpect(status().isNoContent());
        }
        postJson("/accounts/" + id("A") + "/debits", amount(5_000, id("TX-2"))).andExpect(status().isUnprocessableContent());

        mvc.perform(get("/reports/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[?(@.transactionId == '%s')]".formatted(id("TX-1")), hasSize(1)))
                .andExpect(jsonPath("$.transactions[?(@.transactionId == '%s')].type".formatted(id("TX-1"))).value(contains("CREDIT")))
                .andExpect(jsonPath("$.transactions[?(@.transactionId == '%s')].amount".formatted(id("TX-1"))).value(contains(100)))
                .andExpect(jsonPath("$.transactions[?(@.transactionId == '%s')].recordedAt".formatted(id("TX-1")), hasSize(1)))
                .andExpect(jsonPath("$.transactions[?(@.transactionId == '%s')]".formatted(id("TX-2")), empty()));
    }

    @Test
    void livePagesAreServed() throws Exception {
        mvc.perform(get("/reports/accounts.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("/reports/accounts")));
        mvc.perform(get("/reports/transactions.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("/reports/transactions")));
    }

    private String id(String name) {
        return prefix + "-" + name;
    }

    private void open(String accountId, long initialBalance) throws Exception {
        postJson("/accounts", "{\"accountId\": \"%s\", \"initialBalance\": %d}".formatted(accountId, initialBalance))
                .andExpect(status().isCreated());
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String amount(long amount, String transactionId) {
        return "{\"amount\": %d, \"transactionId\": \"%s\"}".formatted(amount, transactionId);
    }
}
