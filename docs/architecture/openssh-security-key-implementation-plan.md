# Plan: OpenSSH Security-Key Algorithms in Maverick Synergy

## Executive Summary
Yes, OpenSSH security-key algorithms are implementable in Java for Maverick Synergy.

Recommended approach:
- Implement in phases.
- Prioritize verification support first (server-side and known-host/cert parsing paths).
- Add client signing support via a pluggable hardware provider bridge, most likely libfido2 through JNA/JNI.

Outcomes:
- `sk-ssh-ed25519@openssh.com`
- `sk-ecdsa-sha2-nistp256@openssh.com`
- Optional follow-on: cert variants and webauthn variants.

## Constraints and Reality Check
1. Pure Java JCA does not directly expose SSH `sk-*` behavior end-to-end.
2. OpenSSH `sk-*` signatures include extra SSH fields (flags/counter and optional webauthn wrapper data), so this is not only "use Ed25519/ECDSA".
3. Hardware-backed signing needs CTAP/FIDO interaction. In Java this is best handled by:
- A native bridge to libfido2 (recommended).
- Or a custom HID/CTAP stack (high effort, high risk).

## Why This Is Feasible in Synergy
Synergy already has extension points that match this work:
- Key parsing/verification abstraction via `SshPublicKey` and factories.
- Signature path abstraction via `SshPrivateKey` and `SignatureGenerator`.
- ServiceLoader-based component registration.

Relevant existing interfaces/classes:
- [maverick-base/src/main/java/com/sshtools/common/ssh/components/SshPublicKey.java](maverick-base/src/main/java/com/sshtools/common/ssh/components/SshPublicKey.java)
- [maverick-base/src/main/java/com/sshtools/common/ssh/components/SshPrivateKey.java](maverick-base/src/main/java/com/sshtools/common/ssh/components/SshPrivateKey.java)
- [maverick-base/src/main/java/com/sshtools/common/publickey/SignatureGenerator.java](maverick-base/src/main/java/com/sshtools/common/publickey/SignatureGenerator.java)

## Scope Decisions (from your guidance)
Included:
- OpenSSH security-key algorithms only.

Excluded:
- Legacy MAC additions (`hmac-sha1-96-etm@openssh.com`, `hmac-md5-96-etm@openssh.com`).
- Hostbased authentication.

Advisory only:
- `gssapi-with-mic` exploration (see section below).

## Proposed Architecture
### A. Key Types and Parsers
Add `SshPublicKey` implementations for:
- `sk-ssh-ed25519@openssh.com`
- `sk-ecdsa-sha2-nistp256@openssh.com`

Optional next:
- `sk-ssh-ed25519-cert-v01@openssh.com`
- `sk-ecdsa-sha2-nistp256-cert-v01@openssh.com`
- `webauthn-sk-ecdsa-sha2-nistp256@openssh.com`

Each implementation must:
1. Parse SSH key blob fields (including SK application and key-handle/reserved fields as required by OpenSSH format).
2. Verify OpenSSH signature structure with SK-specific fields.
3. Expose algorithm/signing names through existing contracts.

### B. Signing Provider Abstraction
Introduce an internal provider SPI for SK operations:
- enroll (optional phase)
- sign
- load resident keys (optional)

Recommended first provider:
- `LibFido2SkProvider` using JNA/JNI bindings to libfido2.

Fallback provider:
- `NoOpSkProvider` that reports unsupported cleanly, allowing verification-only deployments.

### C. Client Integration
1. Add `PublicKeyAuthenticator` compatibility for `sk-*` keys using existing signature flow.
2. Support external signature path first (agent/provider backed), then optional private-key file handling for OpenSSH SK key references.
3. Add config controls:
- enable/disable SK algorithms
- provider path / selection
- RP ID/application override
- PIN and user-presence/user-verification policies

### D. Server Integration
1. Ensure public key authentication verifier can validate `sk-*` signatures using new key implementations.
2. Ensure authorized_keys parsing accepts `sk-*` entries.
3. Support cert-based SK auth once cert variants are implemented.

## Phased Delivery Plan
## Phase 0: Design Spike (1-2 weeks)
Goals:
- Finalize wire-format mapping for key and signature payloads.
- Decide JNA vs JNI bridge approach.
- Build a tiny proof-of-concept signer against one hardware token.

Exit criteria:
- Can parse one `sk-ssh-ed25519` public key and verify one captured signature test vector.
- Can produce one signature from token via provider bridge in a standalone test harness.

Go/No-Go:
- If provider bridge is unstable across macOS/Linux CI runners, defer signing and ship verification-only first.

## Phase 1: Verification-Only Support (2-3 weeks)
Goals:
- Add `SshPublicKey` implementations for both SK algorithms.
- Support parsing in key utilities and authorized_keys paths.
- Server-side acceptance of `sk-*` keys for publickey auth (verification only).

Tests:
- Unit tests for blob parse/serialize roundtrip.
- Signature verification vectors (valid/invalid flags/counter, malformed blobs).
- Integration tests for server auth using captured known-good signatures.

Exit criteria:
- Server verifies `sk-*` signatures and authenticates users with `sk-*` authorized keys.

## Phase 2: Client Signing via Provider Bridge (3-5 weeks)
Goals:
- Implement pluggable SK provider SPI.
- Implement libfido2-backed provider.
- Integrate with `PublicKeyAuthenticator` signing path.

Tests:
- Provider contract tests with mock provider.
- Optional hardware-in-loop CI lane (manual or nightly).
- End-to-end client auth tests against OpenSSH server requiring `sk-*` key.

Exit criteria:
- Synergy client can authenticate to OpenSSH using `sk-ssh-ed25519` and `sk-ecdsa-sha2-nistp256`.

## Phase 3: Cert and WebAuthn Variants (optional, 2-4 weeks)
Goals:
- Add SK cert variants and webauthn variant support.
- Extend policy controls and docs.

Exit criteria:
- Full OpenSSH SK family parity for commonly deployed variants.

## Test Strategy
1. Deterministic vector tests:
- key parse/serialize
- signature parse/verify with flags/counter

2. Cross-interop tests:
- OpenSSH-generated keys/signatures verified by Synergy.
- Synergy client auth accepted by OpenSSH server.

3. Hardware tests:
- at least one YubiKey/FIDO2 token path
- PIN/no-PIN
- user presence / user verification variations

4. Negative tests:
- wrong RP/application
- stale/malformed counters and flags
- truncated/extra fields

## Security and Operational Considerations
1. Never persist token secrets in Synergy.
2. Keep PIN handling out of logs and minimize in-memory lifetime.
3. Treat provider loading as a high-risk boundary:
- signed binaries where possible
- strict path controls
- allowlist for provider locations
4. Add policy toggles to disable SK algorithms if compliance requires.

## Delivery Risks
1. Native dependency management (libfido2 availability and packaging).
2. CI reproducibility for hardware-dependent tests.
3. Platform behavior variance (macOS vs Linux).

Mitigations:
- SPI + provider separation
- verification-only mode independent of hardware
- hardware tests as separate lane

## GSSAPI-with-MIC: Should You Explore It?
Recommendation: explore only if you have enterprise demand (Kerberos/AD SSO requirement).

Pros:
- Valuable for Windows/AD-centric environments with strict SSO policy.

Cons:
- High implementation and ops complexity in Java.
- Hard to test/reproduce reliably in CI.
- Larger support burden than SK algorithms for many teams.

Suggested decision gate:
- If >=2 target customers/environments require Kerberos SSO in next 12 months, run a 1-week feasibility spike.
- Otherwise defer and focus on SK algorithms first.

## Concrete Next Actions
1. Approve Phase 0 spike.
2. Choose provider strategy:
- Preferred: libfido2 bridge (JNA first, JNI fallback).
3. Create feature flag set and provider SPI module skeleton.
4. Implement verification-only SK key classes first to de-risk parsing/verification.
