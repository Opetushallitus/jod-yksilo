# TMT Mock API

Barebones mock API for local development and testing. Implements two endpoints:

* `GET /authorize?redirectUrl=<response-uri>` - simulates OAuth-like implicit token grant
  authorization, returning a JWT
  token in the `token` query parameter of the redirect URL.
* `PUT /v1/profile` -- simulates profile import, requires the JWT token from the previous step.

## Usage

```
gradle :tmt-mock:build bootRun --args='--spring.docker.compose.profiles.active=mock <other args>'
```

In addition, the following configuration needs to be available:

```yaml
jod:
  tmt:
    enabled: true
    token-issuer: "issuer"
    api-url: "http://localhost:8580/v1/profile"
    authorization-url: "http://localhost:8580/authorize"
    kipa-subscription-key: "some-key"
```
