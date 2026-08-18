Note: Changelog started from version 1.0.2.

1.1.0 / (WIP)
==================

Bug Fixes:
* [SELVSUP-40](https://openlmis.atlassian.net/browse/SELVSUP-40) Fixed multiple orders error by introducing atomic sequence generation using PostgreSQL UPSERT (facility_order_sequence table)

Improvements:
* [SELV3-847](https://openlmis.atlassian.net/browse/SELV3-847) Capture the exchange rate when a shipment is created, as the single capture point for every order, and remove the order-create hook.
* [SELV3-844](https://openlmis.atlassian.net/browse/SELV3-844) Add exchange rate model and endpoints.
* [SELV3-846](https://openlmis.atlassian.net/browse/SELV3-846) Add endpoint to capture additional shipment fields (number of volumes, number of ice packs, person responsible for packing, truck and trailer registration, security seal) at shipment confirmation.
* Fixed all errors upon Gradle build command (Checkstyle, PMD, test compilation)
* Added Flyway extension migration for facility_order_sequence table (**requires openlmis-fulfillment >= 9.3.2**)
* Updated Lombok to 1.18.22
* Updated dev Docker image to openlmis/dev:10
* Updated test builders for compatibility with openlmis-fulfillment 9.0.1
