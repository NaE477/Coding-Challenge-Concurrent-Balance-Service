package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.Profiles;
import com.code.challenge.mofid.service.*;
import com.code.challenge.mofid.service.implementations.BalanceServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile(Profiles.POSTGRES)
class JdbcBackendConfiguration {

    @Bean
    BalanceService balanceService(CreditService creditService, DebitService debitService,
                                  TransferService transferService, BalanceQueryService balanceQueryService) {
        return new BalanceServiceImpl(creditService, debitService, transferService, balanceQueryService);
    }
}
