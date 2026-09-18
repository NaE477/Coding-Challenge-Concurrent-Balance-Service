package com.code.challenge.mofid.repository;

import com.code.challenge.mofid.Profiles;
import com.code.challenge.mofid.model.AccountBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
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
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM accounts WHERE id = :id AND NOT deleted)")
                .param("id", accountId)
                .query(Boolean.class)
                .single();
    }

    /** True if the id belongs to a deleted account: the row stays as a tombstone so the id can't be reused. */
    public boolean isDeleted(String accountId) {
        return Boolean.TRUE.equals(jdbc.sql("SELECT EXISTS (SELECT 1 FROM accounts WHERE id = :id AND deleted)")
                .param("id", accountId)
                .query(Boolean.class)
                .single());
    }

    /** Returns false if the account doesn't exist or is already deleted. */
    public boolean markDeleted(String accountId) {
        return jdbc.sql("UPDATE accounts SET deleted = true WHERE id = :id AND NOT deleted")
                .param("id", accountId)
                .update() == 1;
    }

    public Optional<Long> findBalance(String accountId) {
        return jdbc.sql("SELECT balance FROM accounts WHERE id = :id AND NOT deleted")
                .param("id", accountId)
                .query(Long.class)
                .optional();
    }

    public List<AccountBalance> findAllBalances() {
        return jdbc.sql("SELECT id, balance FROM accounts WHERE NOT deleted ORDER BY id")
                .query((row, rowNumber) -> new AccountBalance(row.getString("id"), row.getLong("balance")))
                .list();
    }

    public boolean deposit(String accountId, long amount) {
        return jdbc.sql("""
                        UPDATE accounts SET balance = balance + :amount
                        WHERE id = :id AND NOT deleted AND balance <= :max - :amount""")
                .param("id", accountId)
                .param("amount", amount)
                .param("max", Long.MAX_VALUE)
                .update() == 1;
    }

    public boolean withdraw(String accountId, long amount) {
        return jdbc.sql("UPDATE accounts SET balance = balance - :amount WHERE id = :id AND NOT deleted AND balance >= :amount")
                .param("id", accountId)
                .param("amount", amount)
                .update() == 1;
    }
}
