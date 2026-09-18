package com.code.challenge.mofid.repository;

import com.code.challenge.mofid.Profiles;
import com.code.challenge.mofid.model.TransactionRecord;
import com.code.challenge.mofid.model.TransactionRequest;
import com.code.challenge.mofid.model.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
@Profile(Profiles.POSTGRES)
@RequiredArgsConstructor
public class TransactionRepository {

    private static final String AMOUNT = "amount";

    private final JdbcClient jdbc;

    /**
     * While another open transaction holds an uncommitted row with the same id, this waits for it to commit or roll back.
     */
    public boolean insertIfAbsent(String transactionId, TransactionRequest request) {
        return jdbc.sql("""
                        INSERT INTO transactions (transaction_id, type, source_account_id, destination_account_id, amount)
                        VALUES (:transactionId, :type, :source, :destination, :amount)
                        ON CONFLICT (transaction_id) DO NOTHING""")
                .param("transactionId", transactionId)
                .param("type", request.type().name())
                .param("source", request.sourceAccountId(), Types.VARCHAR)
                .param("destination", request.destinationAccountId(), Types.VARCHAR)
                .param(AMOUNT, request.amount())
                .update() == 1;
    }

    public Optional<TransactionRequest> findRequest(String transactionId) {
        return jdbc.sql("""
                        SELECT type, source_account_id, destination_account_id, amount
                        FROM transactions WHERE transaction_id = :transactionId""")
                .param("transactionId", transactionId)
                .query((row, rowNumber) -> new TransactionRequest(
                        TransactionType.valueOf(row.getString("type")),
                        row.getString("source_account_id"),
                        row.getString("destination_account_id"),
                        row.getLong(AMOUNT)))
                .optional();
    }

    public List<TransactionRecord> findAll() {
        return jdbc.sql("""
                        SELECT transaction_id, type, source_account_id, destination_account_id, amount, created_at
                        FROM transactions ORDER BY created_at, transaction_id""")
                .query((row, rowNumber) -> new TransactionRecord(
                        row.getString("transaction_id"),
                        TransactionType.valueOf(row.getString("type")),
                        row.getString("source_account_id"),
                        row.getString("destination_account_id"),
                        row.getLong(AMOUNT),
                        row.getTimestamp("created_at").toInstant()))
                .list();
    }
}
