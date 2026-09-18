package com.code.challenge.mofid.service;

public interface TransferService {

    void transfer(String sourceAccountId, String destinationAccountId, long amount, String transactionId);
}
