# API Gateway (`APIgateway`) — C4 Level 3 Component View (As Implemented)

This document describes the current internal component architecture at C4 Level 3 based on implemented classes and configuration.

---

## 1. Current Component Responsibilities

The gateway has two classes; the rest of its behavior comes from Spring Cloud configuration.

* **`APIgatewayApplication`**
   * Spring Boot entry point with `@EnableDiscoveryClient`.
* **Route definitions** (`application.properties`)
   * Six routes mapping path prefixes to `lb://` service names (see `routing-and-security.md`).
* **`JwtAuthenticationGatewayFilterFactory`**
   * Route filter `JwtAuthentication`: validates the Bearer token with `security.jwt.secret` and adds `X-User-Name` and `X-Gateway-Secret`.
* **Spring Cloud LoadBalancer**
   * Resolves `lb://` URIs to instances fetched from Eureka (round robin).
* **Eureka Client**
   * Registers the gateway and fetches the registry from `discovery-service`.
* **Netty HTTPS server**
   * TLS termination with `sidis-keystore.p12`.

External dependencies:

* `discovery-service` (Eureka nodes `8761` / `8762`).
* The six business services, called over HTTP.

---

## 2. Architecture Diagram Source

PlantUML source file:

* `architecture-c4-level3.puml`

---

## 3. Data Flow and Dependencies

1. The client sends an HTTPS request to port `7979`.
2. The route predicates select a route by path prefix.
3. On protected routes, `JwtAuthenticationGatewayFilterFactory` validates the token and adds the two headers; on failure it ends the exchange with `401`.
4. The LoadBalancer picks an instance of the target service from the Eureka registry.
5. The request is forwarded over HTTP, and the service response is returned to the client unchanged.
