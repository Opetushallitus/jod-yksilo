# TMT Mock API

Barebones mock API for local development and testing. Implements two endpoints:

* `GET /authorize?redirect_uri=<response-uri>` - simulates OAuth-like implicit token grant
  authorization, returning a authorization code via redirect to the provided response-uri.
* `POST /v1/request-token` -- simulates token request, returning a JWT token given the
  authorization code from the previous step.
* `PUT /v1/profile` -- simulates profile import, requires the JWT token from the token request step.
* `GET /v1/profile` -- simulates profile export, requires the JWT token from the token request step.


The following configuration needs to be available (e.g. in application-local.yaml)

```yaml
jod:
  tmt:
    enabled: true
    export-api: #using mock implementation
      api-url: "http://localhost:8580/v1/profile"
      authorization-url: "http://localhost:8580/authorize"
      token-url: "http://localhost:8580/v1/request-token"
      client-id: "dummy-client-id"
      client-secret: "dummy-client-secret"
      kipa-subscription-key: "dummy-key"
    import-api: #using mock implementation
      api-url: "http://localhost:8580/v1/profile"
      authorization-url: "http://localhost:8580/authorize"
      token-url: "http://localhost:8580/v1/request-token"
      client-id: "dummy-client-id"
      client-secret: "dummy-client-secret"
      kipa-subscription-key: "dummy-key"
```
