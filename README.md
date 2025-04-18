# qti-validator

A one‑class, zero‑dependency micro‑service that validates any QTI 3 or QTI 2.1 XML
against the official XSDs.

## Build & run

    ./gradlew run

Service starts on `http://localhost:8080/validate`.

## Examples

Validate a QTI 3 item:

    curl -X POST --data-binary @item.xml \
         "http://localhost:8080/validate?schema=qti3"

Validate a QTI 2.1 test:

    curl -X POST --data-binary @test.xml \
         "http://localhost:8080/validate?schema=qti21"

Validate with a custom schema path:

    curl -X POST --data-binary @doc.xml \
         "http://localhost:8080/validate?schema=/path/to/other.xsd" 