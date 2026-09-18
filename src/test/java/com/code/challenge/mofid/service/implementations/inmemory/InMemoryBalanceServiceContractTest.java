package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.service.BalanceServiceContractTest;

class InMemoryBalanceServiceContractTest extends BalanceServiceContractTest {

    @Override
    protected Backend createBackend() {
        InMemoryFixture fixture = new InMemoryFixture();
        return new Backend(fixture.balanceService, fixture.accounts);
    }
}
