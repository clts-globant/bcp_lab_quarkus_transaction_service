# Transactions service

This project uses Quarkus.

This microservice manages basic transactions and transaction history, relying on the following microservices:
https://github.com/clts-globant/bcp_lab_quarkus_account_service
https://github.com/clts-globant/bcp_lab_quarkus_customer_service

Features:

* Process a simple transaction

`
curl -v -X POST http://localhost:8083/api/transactions/transfer \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $JWT_TOKEN" \
-d '{
"sourceAccountId": "acc-src-12345",
"targetAccountId": "acc-dest-67890",
"amount": 150.75
}'
`

Basic validations are checked, like existing account Ids, ACTIVE status, enough balance on source account (debit),
and amount always as positive (don't try negative values, system won't allow you).

* Get transaction details

`
curl -v -X GET http://localhost:8083/api/transactions/{transaction_id} \
-H "Authorization: Bearer $JWT_TOKEN"
`

Transactions normally have an UUID, like `123e4567-e89b-12d3-a456-426614174000`. Details include source account
id, target account id, amount that was moved from source to target, timestamp and status (for now, all are
`COMPLETED`)

* Transaction history for an account

`
curl -v -X GET http://localhost:8083/api/transactions/account/{account_id} \
-H "Authorization: Bearer $JWT_TOKEN"
`

Similar results to querying a transaction, but it's all the ones related to the provided account id

Basic health checks (like `q/health`) and metrics are supported thanks to Quarkus/micrometer.
Read https://quarkus.io/guides/management-interface-reference for more details.

## Unit/integration tests
Run
```shell script
./mvnw test
```

## Before running

Don't forget to boot up the aforementioned services (account-service and customer-service) plus a Kafka cluster.
Respective ports/URLs can be configured in `src/main/resources/application.yml`

## Running the application in dev mode

You can run your application in dev mode + live coding:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
Not an uber jar, as the dependencies are copied into the `target/quarkus-app/lib/` directory.

Run with `java -jar target/quarkus-app/quarkus-run.jar`.

For uber jar:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

Run with `java -jar target/*-runner.jar`.

## Creating a native executable

```shell script
./mvnw package -Dnative
```

Application not tested with native build, so far.

You can run the native executable build in a container with:
```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Then simply execute with: `./target/account-service-1.0.0-SNAPSHOT-runner`

## Generating a valid JWT

The JWT is only checked for completeness + the ROLE_ADMIN role/permission, not for full
authorization+authentication, as a full identity system wasn't implemented for the whole solution.
Otherwise, it should be usable.

Instructions to generate a key pair: https://techdocs.akamai.com/iot-token-access-control/docs/generate-rsa-keys
Instructions to generate a JWT wit said keys: https://techdocs.akamai.com/iot-token-access-control/docs/generate-jwt-rsa-key