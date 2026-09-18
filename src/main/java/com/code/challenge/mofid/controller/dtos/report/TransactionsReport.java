package com.code.challenge.mofid.controller.dtos.report;

import com.code.challenge.mofid.model.TransactionRecord;

import java.util.List;

public record TransactionsReport(int count, List<TransactionRecord> transactions) {
}
