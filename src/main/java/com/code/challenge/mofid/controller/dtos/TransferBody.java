package com.code.challenge.mofid.controller.dtos;

public record TransferBody(String sourceAccountId, String destinationAccountId, long amount, String transactionId) {
}
