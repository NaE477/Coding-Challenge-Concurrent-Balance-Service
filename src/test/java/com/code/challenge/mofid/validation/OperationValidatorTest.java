package com.code.challenge.mofid.validation;

import com.code.challenge.mofid.exception.InvalidAmountException;
import com.code.challenge.mofid.exception.InvalidRequestException;
import com.code.challenge.mofid.exception.SameAccountTransferException;
import com.code.challenge.mofid.model.TransactionRequest;
import com.code.challenge.mofid.model.TransactionType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.code.challenge.mofid.validation.OperationValidator.MAX_ID_LENGTH;
import static org.assertj.core.api.Assertions.*;

class OperationValidatorTest {

    private static final String LONGEST_ID = "a".repeat(MAX_ID_LENGTH);
    private static final String TOO_LONG_ID = "a".repeat(MAX_ID_LENGTH + 1);

    @Nested
    class Amount {

        @ParameterizedTest
        @ValueSource(longs = {0, -1, Long.MIN_VALUE})
        void nonPositiveAmountIsRejectedByEveryOperation(long amount) {
            assertThatThrownBy(() -> OperationValidator.validateCredit("A", amount, "TX-1"))
                    .isInstanceOf(InvalidAmountException.class);
            assertThatThrownBy(() -> OperationValidator.validateDebit("A", amount, "TX-1"))
                    .isInstanceOf(InvalidAmountException.class);
            assertThatThrownBy(() -> OperationValidator.validateTransfer("A", "B", amount, "TX-1"))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        void smallestPositiveAmountIsAccepted() {
            assertThat(OperationValidator.validateCredit("A", 1, "TX-1").amount()).isEqualTo(1);
        }
    }

    @Nested
    class Ids {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void blankIdIsRejectedInEveryPosition(String id) {
            assertThatThrownBy(() -> OperationValidator.validateCredit(id, 100, "TX-1"))
                    .isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> OperationValidator.validateCredit("A", 100, id))
                    .isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> OperationValidator.validateDebit(id, 100, "TX-1"))
                    .isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> OperationValidator.validateDebit("A", 100, id))
                    .isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> OperationValidator.validateTransfer(id, "B", 100, "TX-1"))
                    .isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> OperationValidator.validateTransfer("A", id, 100, "TX-1"))
                    .isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> OperationValidator.validateTransfer("A", "B", 100, id))
                    .isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> OperationValidator.validateAccountId(id))
                    .isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> OperationValidator.validateOpenAccount(id, 0))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        void idLongerThanTheColumnIsRejected() {
            assertThatThrownBy(() -> OperationValidator.validateCredit(TOO_LONG_ID, 100, "TX-1"))
                    .isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> OperationValidator.validateCredit("A", 100, TOO_LONG_ID))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        void idOfExactlyMaxLengthIsAccepted() {
            assertThatNoException().isThrownBy(() -> OperationValidator.validateCredit(LONGEST_ID, 100, LONGEST_ID));
        }

        @Test
        void idsAreOpaqueAndNotTrimmed() {
            TransactionRequest request = OperationValidator.validateCredit(" A", 100, "TX-1");

            assertThat(request.destinationAccountId()).isEqualTo(" A");
        }
    }

    @Nested
    class CheckOrder {

        @Test
        void invalidIdIsReportedBeforeInvalidAmount() {
            assertThatThrownBy(() -> OperationValidator.validateDebit("", 0, "TX-1"))
                    .isExactlyInstanceOf(InvalidRequestException.class);
        }

        @Test
        void invalidAmountIsReportedBeforeSameAccountTransfer() {
            assertThatThrownBy(() -> OperationValidator.validateTransfer("A", "A", 0, "TX-1"))
                    .isInstanceOf(InvalidAmountException.class);
        }
    }

    @Nested
    class Transfer {

        @Test
        void transferToTheSameAccountIsRejected() {
            assertThatThrownBy(() -> OperationValidator.validateTransfer("A", "A", 100, "TX-1"))
                    .isInstanceOf(SameAccountTransferException.class)
                    .hasFieldOrPropertyWithValue("accountId", "A");
        }

        @Test
        void accountIdsDifferingOnlyByCaseAreDifferentAccounts() {
            assertThatNoException().isThrownBy(() -> OperationValidator.validateTransfer("a", "A", 100, "TX-1"));
        }
    }

    @Nested
    class ProducedRequest {

        @Test
        void creditHasOnlyADestination() {
            assertThat(OperationValidator.validateCredit("A", 100, "TX-1"))
                    .isEqualTo(new TransactionRequest(TransactionType.CREDIT, null, "A", 100));
        }

        @Test
        void debitHasOnlyASource() {
            assertThat(OperationValidator.validateDebit("A", 100, "TX-1"))
                    .isEqualTo(new TransactionRequest(TransactionType.DEBIT, "A", null, 100));
        }

        @Test
        void transferHasBothAccounts() {
            assertThat(OperationValidator.validateTransfer("A", "B", 100, "TX-1"))
                    .isEqualTo(new TransactionRequest(TransactionType.TRANSFER, "A", "B", 100));
        }

        @Test
        void identicalInputProducesEqualRequests() {
            assertThat(OperationValidator.validateTransfer("A", "B", 100, "TX-1"))
                    .isEqualTo(OperationValidator.validateTransfer("A", "B", 100, "TX-1"));
        }

        @Test
        void creditAndDebitOfTheSameAccountAndAmountAreDifferentRequests() {
            assertThat(OperationValidator.validateCredit("A", 100, "TX-1"))
                    .isNotEqualTo(OperationValidator.validateDebit("A", 100, "TX-1"));
        }

        @Test
        void reversedTransferIsADifferentRequest() {
            assertThat(OperationValidator.validateTransfer("A", "B", 100, "TX-1"))
                    .isNotEqualTo(OperationValidator.validateTransfer("B", "A", 100, "TX-1"));
        }
    }

    @Nested
    class OpenAccount {

        @Test
        void zeroInitialBalanceIsAccepted() {
            assertThatNoException().isThrownBy(() -> OperationValidator.validateOpenAccount("A", 0));
        }

        @Test
        void negativeInitialBalanceIsRejected() {
            assertThatThrownBy(() -> OperationValidator.validateOpenAccount("A", -1))
                    .isInstanceOf(InvalidAmountException.class);
        }
    }
}
