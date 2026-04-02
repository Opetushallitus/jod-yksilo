# ONR Mock API

Barebones mock API for local development and testing. Implements:

* `POST /oauth2/token` - mock OAuth2 client_credentials token endpoint
* `POST /yleistunniste/hae` - mock ONR yleistunniste lookup, returns a deterministic OID based on hetu

The following configuration needs to be available (e.g. in application-local.yaml)

```yaml
jod:
  onr:
    base-url: "http://localhost:8480"

spring:
  security:
    oauth2:
      client:
        registration:
          onr:
            client-id: "dummy-client-id"
            client-secret: "dummy-client-secret"
        provider:
          onr:
            token-uri: "http://localhost:8480/oauth2/token"
```
