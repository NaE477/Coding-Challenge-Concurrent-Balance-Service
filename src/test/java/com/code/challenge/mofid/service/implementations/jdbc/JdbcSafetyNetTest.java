package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.model.TransactionRequest;
import com.code.challenge.mofid.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.IllegalTransactionStateException;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class JdbcSafetyNetTest {

    @BeforeEach
    void emptyTables() {
        PostgresBackend.emptyTables();
    }

    @Test
    void ledgerRefusesToRunOutsideADatabaseTransaction() {
        JdbcTransactionLedger ledger = PostgresBackend.bean(JdbcTransactionLedger.class);
        TransactionRequest request = new TransactionRequest(TransactionType.CREDIT, null, "A", 100);

        assertThatThrownBy(() -> ledger.executeOperation("TX-1", request, () -> { }))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void databaseRejectsANegativeBalanceEvenWhenTheApplicationDoesNotCheck() {
        JdbcClient jdbc = PostgresBackend.bean(JdbcClient.class);
        jdbc.sql("INSERT INTO accounts (id, balance) VALUES ('A', 100)").update();
        JdbcClient.StatementSpec sql = jdbc.sql("UPDATE accounts SET balance = -1 WHERE id = 'A'");

        assertThatThrownBy(sql::update).isInstanceOf(DataIntegrityViolationException.class);
    }
}
