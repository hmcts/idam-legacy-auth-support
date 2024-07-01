[![Release](https://jitpack.io/v/hmcts/idam-legacy-auth-support.svg)](https://jitpack.io/#hmcts/idam-legacy-auth-support)
[![JitPack Badge](https://github.com/hmcts/idam-legacy-auth-support/actions/workflows/jitpack_build.yml/badge.svg)](https://github.com/hmcts/idam-legacy-auth-support/actions/workflows/jitpack_build.yml)

# IdAM Legacy Auth Support
A Java module to simplify IdAM password grant calls, and S2S lease calls.

## User Guide

This is a library to support legacy/custom api authentication calls for CFT services:

* Password Grants are deprecated in OpenId but still used by the majority of services.
* S2S is a custom service auth solution.

The library provides:

* Feign interceptor for password grants
* Password grants from Hmcts-access are managed by spring security
* Feign interceptor for S2S leases
* Allows for simple integration of S2S testing support

The library is only required for making API calls to other services. It is not required for authentication of users with Hmcts-access.

Once you have integrated with spring security you can user the standard annotations for RBAC. You will still need to use the S2S auth filter included in the rps-s2s library for S2S access control.

Note that the SIDAM team do not maintain S2S, but have included support for it here for simplicity.

The library has auto configuration enabled based on spring properties, so all you need to do to start using it is import the library into 
your spring boot project and apply the correct configuration as described below.

### Spring Password Grants

To add password grant bearer tokens to feign calls in your project set the following values in application.yaml

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

Once that is done then any feign calls that you make that match the `endpoint-regex` will have an Authentication header with the 
password grant bearer token. The fetching/caching of the token is handled by spring security.

When the password grant support is active you will see a message similar to the following on application startup:

```
DefaultPasswordGrantAutoConfiguration idam-legacy-auth-support: Configured defaultPasswordGrantInterceptor for client reference: my-example-service, endpoints: /my-url.*
```

### RPE S2S

To add the S2S `ServiceAuthorization` header to feign calls made by your project add the following in application.yaml

```
idam:
  s2s-auth:
    microservice: my-service-s2s-id
    totp_secret: my-service-s2s-secret
    url: rpe-service-auth-provider-env-url
    endpoint-regex: /my-s2s-url.*
```
Note that you still need to import https://github.com/hmcts/service-auth-provider-java-client/ as well to be able to make S2S calls.

When S2S lease support is active you will see message similar to the following on application startup:

```
RpeS2SAutoConfiguration idam-legacy-auth-support: Configured s2sAuthTokenGenerator for service: my-service-s2s-id
RpeS2SAutoConfiguration idam-legacy-auth-support: Configured rdServiceAuthorizationInterceptor for service: my-service-s2s-id, endpoints: /my-s2s-url.*
```

If you want to run S2S using the testing-support/lease endpoint then you can add the following configuration:
```
idam:
  s2s-auth:
    testing-support:
      enabled: true
```
This can be useful if you are developing before your microservice has been deployed to S2S. When running in testing-support mode 
you will see a message similar to the following at startup:
```
RpeS2SAutoConfiguration idam-legacy-auth-support: Configured s2sTestingSupportAuthTokenGenerator for service: my-service-s2s-id
```

### Making Password Grant calls in tests

This library is not intended to be used for fetching password grant calls in tests. The simplest way to make password grants calls
in tests is to use SerenityRest.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.