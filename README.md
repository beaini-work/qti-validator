# qti-validator

A one‑class, zero‑dependency micro‑service that validates any QTI 3 XML against the official IMS XSDs and returns a **minimal JSON payload** that is easy for a Node.js (or any) client to consume.

## Build & run

Compile once:

```bash
./gradlew build       # produces build/libs/qti-validator.jar
```

Run the service (default port `5000`):

```bash
java -jar build/libs/qti-validator.jar
```

To change the port simply export `PORT`:

```bash
PORT=8080 java -jar build/libs/qti-validator.jar
```

The validation endpoint will be available at `http://localhost:<PORT>/validate`.

## Examples

### Example request

Validate a QTI 3 item (assuming default port 5000):

```bash
curl -X POST \
     -H "Content-Type: application/xml" \
     --data-binary @item.xml \
     "http://localhost:5000/validate?schema=qti3"
```

### JSON response contract

Success (`HTTP 200`):

```json
{ "valid": true }
```

Validation error (`HTTP 422`):

```json
{
  "valid": false,
  "errors": [
    {
      "message": "<human‑readable reason>",
      "line": 12,
      "column": 34
    }
  ]
}
```
