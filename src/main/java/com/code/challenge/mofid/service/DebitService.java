package com.code.challenge.mofid.service;

public interface DebitService {

    void debit(String accountId, long amount, String transactionId);
}
