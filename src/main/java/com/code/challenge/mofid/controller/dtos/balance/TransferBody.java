package com.code.challenge.mofid.controller.dtos.balance;

public record TransferBody(String sourceAccountId, String destinationAccountId, long amount, String transactionId) {
}
