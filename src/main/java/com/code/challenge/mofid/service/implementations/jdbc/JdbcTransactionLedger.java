package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.Profiles;
import com.code.challenge.mofid.exception.IdempotencyConflictException;
import com.code.challenge.mofid.model.TransactionRequest;
import com.code.challenge.mofid.repository.TransactionRepository;
import com.code.challenge.mofid.service.TransactionLedger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile(Profiles.POSTGRES)
@RequiredArgsConstructor
class JdbcTransactionLedger implements TransactionLedger {

    private final TransactionRepository transactions;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void executeOperation(String transactionId, TransactionRequest request, Runnable operation) {
        if (isTransactionExecutable(transactionId, request)) {
            log.debug("Unique transactionId: {} received", transactionId);
            operation.run();
            return;
        }
        TransactionRequest earlierRequest = transactions.findRequest(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction '" + transactionId + "' found earlier disappeared."));
        if (!earlierRequest.equals(request)) {
            throw new IdempotencyConflictException(transactionId);
        }
    }

    private boolean isTransactionExecutable(String transactionId, TransactionRequest request) {
        return transactions.insertIfAbsent(transactionId, request);
    }
}
