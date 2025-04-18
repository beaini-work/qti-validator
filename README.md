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

## Deploy to AWS with Terraform

The repo ships with a minimal Terraform configuration under `terraform/` that provisions:

* an S3 bucket to store versioned application artifacts
* an Elastic Beanstalk application & environment running the JAR

### Prerequisites

* Terraform ≥ 1.5 installed
* AWS credentials exported (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, etc.) with permissions for S3 and Elastic Beanstalk

### Quick‑start

```bash
# 1. Build the JAR (if you haven't already)
./gradlew build

# 2. Switch to the Terraform folder
cd terraform

# 3. Initialise providers & modules
terraform init

# 4. Review changes & deploy
terraform apply    # add -auto-approve if you want to skip the prompt

# 5. Grab the live URL
terraform output eb_environment_url
```

The default variables will deploy into `us‑east‑2` using the Amazon Linux 2023 Corretto 21 platform.

You can override any variable at apply‑time, e.g. to change region or environment name:

```bash
terraform apply -var "aws_region=eu-west-1" -var "environment_name=qti-validator-dev"
```

By default the JAR path is `../build/libs/qti-validator.jar`; if you move the artefact elsewhere pass `-var "jar_path=/absolute/path/to/jar"`.

The service is exposed on port 5000 (or whatever `PORT` you set via Elastic Beanstalk environment variables) and retains the same JSON contract shown above.
