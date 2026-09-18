package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.service.AccountRegistry;
import com.code.challenge.mofid.service.BalanceService;
import com.code.challenge.mofid.service.BalanceServiceContractTest;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgresBalanceServiceContractTest extends BalanceServiceContractTest {

    @Override
    protected Backend createBackend() {
        PostgresBackend.emptyTables();
        return new Backend(PostgresBackend.bean(BalanceService.class), PostgresBackend.bean(AccountRegistry.class));
    }
}
