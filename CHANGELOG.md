Note: Changelog started from version 1.0.2.

1.0.2 / 2026-04-14
==================

Bug Fixes:
* [SELVSUP-40](https://openlmis.atlassian.net/browse/SELVSUP-40) Fixed multiple orders error by introducing atomic sequence generation using PostgreSQL UPSERT (facility_order_sequence table)

Improvements:
* Fixed all errors upon Gradle build command (Checkstyle, PMD, test compilation)
* Added Flyway extension migration for facility_order_sequence table (**requires openlmis-fulfillment >= 9.3.2**)
* Updated Lombok to 1.18.22
* Updated dev Docker image to openlmis/dev:10
* Updated test builders for compatibility with openlmis-fulfillment 9.0.1
