package com.code.challenge.mofid.controller;

import com.code.challenge.mofid.controller.dtos.AmountBody;
import com.code.challenge.mofid.controller.dtos.BalanceResponse;
import com.code.challenge.mofid.controller.dtos.OpenAccountBody;
import com.code.challenge.mofid.controller.dtos.TransferBody;
import com.code.challenge.mofid.service.AccountRegistry;
import com.code.challenge.mofid.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;
    private final AccountRegistry accountRegistry;

    @PostMapping("/accounts")
    public ResponseEntity<BalanceResponse> openAccount(@RequestBody OpenAccountBody body) {
        accountRegistry.openAccount(body.accountId(), body.initialBalance());
        return ResponseEntity.created(URI.create("/accounts/" + body.accountId() + "/balance"))
                .body(new BalanceResponse(body.accountId(), body.initialBalance()));
    }

    @GetMapping("/accounts/{accountId}/balance")
    public BalanceResponse getBalance(@PathVariable String accountId) {
        return new BalanceResponse(accountId, balanceService.getBalance(accountId));
    }

    @PostMapping("/accounts/{accountId}/credits")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void credit(@PathVariable String accountId, @RequestBody AmountBody body) {
        balanceService.credit(accountId, body.amount(), body.transactionId());
    }

    @PostMapping("/accounts/{accountId}/debits")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void debit(@PathVariable String accountId, @RequestBody AmountBody body) {
        balanceService.debit(accountId, body.amount(), body.transactionId());
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transfer(@RequestBody TransferBody body) {
        balanceService.transfer(body.sourceAccountId(), body.destinationAccountId(), body.amount(),
                body.transactionId());
    }
}
