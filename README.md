[![Release](https://jitpack.io/v/hmcts/idam-legacy-auth-support.svg)](https://jitpack.io/#hmcts/idam-legacy-auth-support)
[![JitPack Badge](https://github.com/hmcts/idam-legacy-auth-support/actions/workflows/jitpack_build.yml/badge.svg)](https://github.com/hmcts/idam-legacy-auth-support/actions/workflows/jitpack_build.yml)

# IdAM Legacy Auth Support
A Java module to simplify IdAM password grant calls, and S2S lease calls.

## User Guide

Import the library to your spring boot project

### Spring Password Grants

Set the following in application.yaml

```
idam:
  legacy:
    password-grant:
      registration-reference: my-example-service
      endpoint-regex: /my-url.*
      service-account:
        email-address: my-service-account@test.local
        password: my-service-secret
```

The registration reference refers to the spring security oauth2 client that you will also need to configure, for example:
```
spring:
  security:
      client:
        registration:
          my-example-service:
            authorization-grant-type: password
            client-id: my-service-client-id
            client-secret: my-service-client-secret
            client-authentication-method: client_secret_post
            scope:
              - openid
              - profile
              - roles
```

### RPE S2S

Set the following in application.yaml

```
idam:
  s2s-auth:
    microservice: my-service-s2s-id
    totp_secret: my-service-s2s-secret
    url: http://rpe-service-auth-provider-aat.service.core-compute-aat.internal
    endpoint-regex: /my-url.*
    testing-support:
      enabled: false
```