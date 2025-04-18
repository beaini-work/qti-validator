# qti-validator

A one‑class, zero‑dependency micro‑service that validates any QTI 3 against the official XSDs.

## Build & run

    ./gradlew run

Service starts on `http://localhost:8080/validate`.

## Examples

Validate a QTI 3 item:

    curl -X POST --data-binary @item.xml \
         "http://localhost:8080/validate?schema=qti3"
