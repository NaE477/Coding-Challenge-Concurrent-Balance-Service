package com.code.challenge.mofid.service;

import com.code.challenge.mofid.model.TransactionRequest;

public interface TransactionLedger {

    void executeOperation(String transactionId, TransactionRequest request, Runnable operation);
}
