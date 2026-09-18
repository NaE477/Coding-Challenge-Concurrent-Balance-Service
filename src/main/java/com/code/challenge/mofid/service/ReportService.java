package com.code.challenge.mofid.service;

import com.code.challenge.mofid.model.AccountBalance;
import com.code.challenge.mofid.model.TransactionRecord;

import java.util.List;

public interface ReportService {

    /** Every account, ordered by id. */
    List<AccountBalance> accountBalances();

    /** Every applied transaction, descending sort by creation time. */
    List<TransactionRecord> transactions();
}
