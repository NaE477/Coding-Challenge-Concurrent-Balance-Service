package com.code.challenge.mofid.repository;

import com.code.challenge.mofid.Profiles;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile(Profiles.POSTGRES)
@RequiredArgsConstructor
public class AccountRepository {

    private final JdbcClient jdbc;

    public boolean insertIfAbsent(String accountId, long balance) {
        return jdbc.sql("INSERT INTO accounts (id, balance) VALUES (:id, :balance) ON CONFLICT (id) DO NOTHING")
                .param("id", accountId)
                .param("balance", balance)
                .update() == 1;
    }

    public Boolean exists(String accountId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM accounts WHERE id = :id)")
                .param("id", accountId)
                .query(Boolean.class)
                .single();
    }

    public Optional<Long> findBalance(String accountId) {
        return jdbc.sql("SELECT balance FROM accounts WHERE id = :id")
                .param("id", accountId)
                .query(Long.class)
                .optional();
    }

    public boolean deposit(String accountId, long amount) {
        return jdbc.sql("""
                        UPDATE accounts SET balance = balance + :amount
                        WHERE id = :id AND balance <= :max - :amount""")
                .param("id", accountId)
                .param("amount", amount)
                .param("max", Long.MAX_VALUE)
                .update() == 1;
    }

    public boolean withdraw(String accountId, long amount) {
        return jdbc.sql("UPDATE accounts SET balance = balance - :amount WHERE id = :id AND balance >= :amount")
                .param("id", accountId)
                .param("amount", amount)
                .update() == 1;
    }
}
