# API Gateway — Sequence Diagrams and Dynamic Views (As Implemented)

This document describes runtime behavior aligned with the current code implementation.

---

## 1. Scenario 1: Routed Request

### Description

Scenario 1 shows how the gateway handles a public route (`POST /api/auth/login`) and a protected route (`GET /api/hangar/analytics/status-summary`), including JWT validation and load balancing.

### Sequence Diagram Source

* `scenario-1-routed-request.puml`

### Key Steps

1. The client calls the gateway over HTTPS on port `7979`.
2. The route is selected by path prefix; `/api/auth/**` skips the JWT filter.
3. On protected routes, a missing header, a non-Bearer header, a bad signature or an expired token returns `401` with an empty body.
4. A valid token gets `X-User-Name` (token subject) and `X-Gateway-Secret` added.
5. The LoadBalancer picks an instance of the target service from the Eureka registry; with no instance registered the gateway returns `503`.
6. The request is forwarded over HTTP, and the service response goes back to the client unchanged.

---

## 2. Exception Summary

| Case | Where | HTTP Status |
| :--- | :--- | :--- |
| Path matches no route | API Gateway | `404 Not Found` |
| Missing / non-Bearer / invalid / expired token | API Gateway | `401 Unauthorized` |
| No instance of the target service registered | API Gateway | `503 Service Unavailable` |
| Missing `X-Gateway-Secret` (direct call to a service) | Downstream service | `403 Forbidden` |
| Business errors | Downstream service | As returned by the service |
