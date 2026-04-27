# MPASSid & ONR Mock

Mock OIDC provider and ONR (Oppijanumerorekisteri) API for local development and testing.

## Endpoints

### OIDC (MPASSid)

* `GET /oidc/.well-known/openid-configuration` — OpenID Connect discovery
* `GET /oidc/jwks` — JSON Web Key Set
* `GET /oidc/authorize` — Authorization page with pre-defined test users and custom user form
* `POST /oidc/authorize/login` — Processes login, redirects back with an authorization code
* `POST /oidc/token` — Exchanges authorization code for an ID token (JWT)

### ONR (Oppijanumerorekisteri)

* `POST /onr/oauth2/token` — Client credentials token endpoint (returns a dummy token)
* `PUT /onr/yleistunniste` — Batch OID creation
* `GET /onr/yleistunniste/tuonti={id}` — Fetch batch import results

## Configuration

Add the following to `application-local.yml`:

```yaml
jod:
  mpassid:
    enabled: true
    onr-oid-prefix: "1.2.246.562."
    validate-onr-checksum: false
    oidc:
      client-id: "mpassid-client"
      client-secret: "mpassid-secret"
      issuer-uri: "http://localhost:8380/oidc"
      authorization-uri: "http://localhost:8380/oidc/authorize"
      token-uri: "http://localhost:8380/oidc/token"
      jwks-uri: "http://localhost:8380/oidc/jwks"
      logout-uri: ""
  onr:
    oid-prefix: "1.2.246.562.98."
    base-url: "http://localhost:8380/onr"
    oauth2:
      token-uri: "http://localhost:8380/onr/oauth2/token"
      client-id: "onr-client"
      client-secret: "onr-client-secret"
```
