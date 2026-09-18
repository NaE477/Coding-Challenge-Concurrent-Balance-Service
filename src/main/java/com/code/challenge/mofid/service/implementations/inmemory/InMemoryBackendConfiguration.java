package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.Profiles;
import com.code.challenge.mofid.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * The default backend: active whenever the {@code postgres} profile is not.
 */
@Configuration(proxyBeanMethods = false)
@Profile("!" + Profiles.POSTGRES)
class InMemoryBackendConfiguration {

    @Bean
    InMemoryAccountStore inMemoryAccountStore() {
        return new InMemoryAccountStore();
    }

    @Bean
    InMemoryTransactionLedger inMemoryTransactionLedger() {
        return new InMemoryTransactionLedger();
    }

    @Bean
    CreditService inMemoryCreditService(InMemoryAccountStore accounts, TransactionLedger ledger) {
        return new InMemoryCreditService(accounts, ledger);
    }

    @Bean
    DebitService inMemoryDebitService(InMemoryAccountStore accounts, TransactionLedger ledger) {
        return new InMemoryDebitService(accounts, ledger);
    }

    @Bean
    TransferService inMemoryTransferService(InMemoryAccountStore accounts, TransactionLedger ledger) {
        return new InMemoryTransferService(accounts, ledger);
    }

    @Bean
    BalanceQueryService inMemoryBalanceQueryService(InMemoryAccountStore accounts) {
        return new InMemoryBalanceQueryService(accounts);
    }
}
