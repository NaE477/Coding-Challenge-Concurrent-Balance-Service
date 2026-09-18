package com.code.challenge.mofid.controller.dtos.report;

import com.code.challenge.mofid.model.AccountBalance;

import java.math.BigInteger;
import java.util.List;

public record AccountsReport(int accountCount, BigInteger totalBalance, List<AccountBalance> accounts) {
}
