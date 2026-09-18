package com.code.challenge.mofid.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({InvalidRequestException.class, InvalidAmountException.class, SameAccountTransferException.class})
    public ProblemDetail badRequest(BalanceServiceException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail notFound(BalanceServiceException exception) {
        return problem(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler({IdempotencyConflictException.class, DuplicateAccountException.class})
    public ProblemDetail conflict(BalanceServiceException exception) {
        return problem(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler({InsufficientFundsException.class, BalanceOverflowException.class})
    public ProblemDetail unprocessable(BalanceServiceException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, exception);
    }

    private static ProblemDetail problem(HttpStatus status, BalanceServiceException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setProperty("error", exception.getClass().getSimpleName());
        return problem;
    }
}
