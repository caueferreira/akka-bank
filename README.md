# Akka Bank
> A showcase application using [akka](https://akka.io/ "akka") framework, in order to emulate financials operations, ensuring the idempotence, parallelism and concurrency of its operations. It was also developed with event sourcing in mind

[![CircleCI](https://circleci.com/gh/caueferreira/akka-bank.svg?style=svg)](https://circleci.com/gh/caueferreira/akka-bank) [![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
 [![Maintainability](https://api.codeclimate.com/v1/badges/6df8d9d2452bbe235682/maintainability)](https://codeclimate.com/github/caueferreira/akka-bank/maintainability) [![codebeat badge](https://codebeat.co/badges/c03cd996-266b-4601-a1ca-e7185407578a)](https://codebeat.co/projects/github-com-caueferreira-akka-bank-master) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/2b7a211714e64697bc5f581082479182)](https://www.codacy.com/manual/caueferreira/akka-bank?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=caueferreira/akka-bank&amp;utm_campaign=Badge_Grade) [![codecov](https://codecov.io/gh/caueferreira/akka-bank/branch/master/graph/badge.svg)](https://codecov.io/gh/caueferreira/akka-bank)

## About
This is a showcase application that uses akka framework as the core of its functionality. I decided to use akka as the chosen framework, mostly for its natural ability of helping you to construct an concurrent and distributed application.
The application shows four operations:
*   *READ* ~ given an account, this requests.operation return its balance
*   *CREDIT* ~ given an account, this requests.operation increases its balance
*   *DEBIT* ~ given an account, this requests.operation decreases its balance
*   *TRANSFER* ~ given two accounts - an receiver and an requester -, this requests.operation decreases the balance from the requester and adds it to the receiver balance

## Architecture

Akka-Bank was developed with event sourcing in mind, it has an **EventStore** class, that mocks the events of the accounts and when an **Account** actor is instanciated it will check for his events and compose its balance. The **AccountSupervisor** is responsible for forwarding all events to the responsible actor and the **TransferSaga** is exclusively responsible for handling the transferency business rules. The account actors were developer with idempotency, so if some reason the same requests.operation is to be requested, it will return a proper message instead of re-executing the requests.operation. 

<p align="center">
  <img src="https://github.com/caueferreira/akka-bank/blob/master/.github/actors.png" width="360">
</p>

### How it works?

The **Account** was developed with idempotence in mind, once it is instantiated it gathers all events from itself and compose its balance. Whenever an **Account** handle an event that it has already executed, it will return a message stating that it has been already executed. If it receives an event of Debit but the result of its deduction would reduce its balance below a limit threshold, the **Account** will answer with an error response, it shapes how **TransferSaga** handles transfer events. 

Since the focus of this application is the transfer, most of the other functions do not have full error handling. When **TransferSaga** receives an event of transfer, it will tell both accounts that make part of this transfer either credit or debit, then it will await for a Future response, if both accounts respond with success, it will then answer the **AccountSupervisor** with a success response; however if the account tha would be debited, respond with an error the **TransferSaga** will trigger a compensation, telling the account that was credited to debit the value. Though that might not be the best of the approaches, this showcase how the concurrency could be used, not awaiting for the first response.

The **EventStore** represents the events of all accounts, the idea behind it is to a event sourcing. This application was developed with event driven architecture in mind, all events are stored in the store, those events are immutable.

## Stack
*   Akka
*   Akka-Http
*   Junit
 
## Documentation

https://documenter.getpostman.com/view/7830954/SVmvRyB2

## Running

**Run the server**

`mvn package && java -jar target/akka-bank-1.0-SNAPSHOT-allinone.jar`

**Runing tests**

`mvn test`
