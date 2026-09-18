## Architecture

Project is a simple monolithic, services are separated and coupled loosely enough to scale up to smaller microservices.
There are two profiles to choose between the way application is going to maintain state. In-Memory (default if no profile is set) and `postgres`,
which their name explains the behavior.

Core of the application would be `BalanceService` and the services used inside of it.
`CreditService`, `DebitService`, `TransferService` and `BalanceQueryService` used in `BalanceService` are implemented in 2 different backends
and are activated based on the profile.

## Concurrency

### In-Memory approach

Each account contains its own `ReentrantLock` which is used by the operation that is associating with the account. The idea is per record locking
approach of a database but in the application level. And the reason why there's one lock per account is so that different operations can be done
independently of this one and not get affected by it.
In the scenario which few operations happen for one account, each operation is run by the order they acquire the lock and each sees the latest result
of the one before so no update is lost.
A debit checks the balance while holding the lock, so only as many debits succeed as the balance allows and it never goes negative
(e.g. 1,000 concurrent debits of 150 from 100,000 → exactly 666 succeed, 100 left).

### Database approach

Each withdraw/deposit operation is done in a `Transactional` method and an update query locks the row until the end of the method starting the transaction.
Under Read Commited, an update operation re-checks the row it is modifying after its turn is reached and acquires the lock,
so a lost update won't happen by more than one operation on the same row.

## Idempotency

Transactions are controlled through the `TransactionLedger` interface that controls the idempotency mechanism for each operation that uses it.
It uses the idempotency key (`transactionId`), transaction parties' information, and the operation itself (withdraw/deposit).

### In-Memory approach

This one uses a `ConcurrentHashMap` at its `TransactionLedger` implementation in order to keep track of transactions done in this session of application.
The map is keyed by `transactionId` and values are transaction info (Parties, a `CompletableFuture` as the outcome).
A duplicate with the same request waits on the first attempt's `CompletableFuture` and returns its result, so it never returns before the balance has changed.
The same id with a different request is a conflict. If the operation fails, its entry is removed, so the id can be retried.
A `ConcurrentHashMap`'s `putIfAbsent` is run atomically, so exactly one caller can register a given `transactionId`.

### Database approach

The ledger first inserts the transaction with `INSERT … ON CONFLICT (transaction_id) DO NOTHING`, which never throws; a known id just inserts 0 rows.
The ledger then loads the stored request: if it matches, it's a retry and the call returns without applying anything again;
if it differs, it throws an idempotency exception.
A concurrent duplicate's `INSERT` waits on the primary key until the first transaction commits (then it's a duplicate) or rolls back (then it runs itself).
A failed operation's rollback removes its row too, so the id can be retried.

## Transfer

### Atomicity

#### In-memory

Both accounts are locked, then every check runs before either balance changes, because locks can't undo a half-done change.

#### Database

Both `UPDATE`s run in one transaction; if either fails, the rollback undoes both.

### Deadlocks

In order to prevent deadlocks in a transaction, in both In-Memory and Database approach, there's an order to the locking mechanism so the account with
lower `String.compareTo` is locked first. This will prevent any deadlock cycle associating with the account.

### Same Account Transfer

The OperationValidator class throws an exception if it finds both accounts are equal. The logical decision is that the transaction is either a
mistake by user, and also it won't create any change to the state of the system in any sense, so it's strictly stopped.

## Docker
Containerization was implemented for the sake of an easy run and test of the project in another computer. But from a technical aspect,
this can help deploying the project in a different infrastructure without the need to tune the environment for this specific instance.

# Trade-offs

## Serialization

In both backends, in order to fully prevent deadlocks, accounts are serialized. This is a huge scaling ceiling. Independent accounts scale linearly
but hot accounts will not.

## In-memory backend's ledger
By nature, an in-memory state store is limited by the memory allocated to the application, so a long-running process with many transactions
will eventually run out of memory, and it's fit for testing purposes.

Database scaling is also another matter above the scale of this challenge.

# How to use
Project is dockerized and has actuator for observability:

```bash
docker compose up --build     # API on http://localhost:8080
docker compose down -v        # stop and delete the data
```

Tests can be run with these commands on windows with Java 21:

```bash
mvnw.cmd test
mvnw.cmd verify
```

or in linux:
```bash
./mvnw test          # must pass from a clean clone
./mvnw verify
```
`test` commands can be run without application being up in docker and skip psql tests, but `verify` commands will run everything with docker up.

Controllers can be accessed through a postman collection put in a package with the same name. The JSON file can be imported and used in a postman software
to test the functionality of the project live.

Also, two simple UIs are accessible through http://localhost:8080/reports/accounts.html and http://localhost:8080/reports/transactions.html
to observe the ongoing reports visually.

# Use of AI

Project was built with an AI coding assistant (Claude Code), which challenge permits.
Main part of the AI was implementing the architecture and decisions handed to it as its roadmap and drafting the code which awaited my approval.
My part was deciding the architecture and advice the AI on clean code at some points, approve or modify draft code, and
instruct each phase step by step (over more than 20 decisions that needed supervision)
Writing the tests in a comprehensive way was also drafted by the AI but off of my idea for each scenario that could occur.