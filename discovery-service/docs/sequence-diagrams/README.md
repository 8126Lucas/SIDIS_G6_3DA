# Discovery Service — Sequence Diagrams and Dynamic Views (As Implemented)

This document describes runtime behavior aligned with the current code implementation.

---

## 1. Scenario 1: Register and Resolve

### Description

Scenario 1 shows a service instance registering with Eureka, the registration being replicated to the peer node, and the gateway using the registry to route a request to that instance.

### Sequence Diagram Source

* `scenario-1-register-and-resolve.puml`

### Key Steps

1. On startup, the service sends its registration (name, host or IP, port) to the first node in its `defaultZone`.
2. The node stores the lease and replicates it to its peer (only when the peer URL is reachable; see `registry-topology.md`, section 3).
3. The service sends a heartbeat every 30 seconds; without heartbeats for 90 seconds the lease expires (unless self-preservation is active).
4. The gateway fetches the registry every 30 seconds and keeps a local copy.
5. A request to `lb://<service>` is resolved from that local copy, so a new instance can take up to about a minute to receive traffic.
6. On a normal shutdown, the client cancels its lease.

---

## 2. Exception Summary

| Case | Effect |
| :--- | :--- |
| First node in `defaultZone` unreachable | Client retries with the next node in the list |
| No node reachable | Client keeps its last fetched registry and retries in the background |
| Service not yet in the gateway's local registry | Gateway returns `503 Service Unavailable` |
| Instance stops without cancelling its lease | Still listed until the lease expires; requests to it fail meanwhile |
