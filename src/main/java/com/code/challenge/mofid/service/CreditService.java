package com.code.challenge.mofid.service;

public interface CreditService {

    void credit(String accountId, long amount, String transactionId);
}
