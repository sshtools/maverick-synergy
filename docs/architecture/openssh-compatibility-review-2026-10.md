# OpenSSH vs Maverick Synergy Specification Review (Client and Server)

## Executive Summary
Maverick Synergy 3.2 has strong OpenSSH interoperability for transport algorithms and OpenSSH-specific protocol behaviors on both client and server roles.

Key findings:
- Key exchange (KEX): Full parity with the OpenSSH KEX list observed on this host, plus Synergy adds extra algorithms (including `mlkem768nistp256-sha256` and `mlkem1024nistp384-sha384` via `maverick-bc`).
- Ciphers: Full parity with the OpenSSH cipher list observed on this host, plus extra legacy ciphers in Synergy.
- MACs: Near parity, but Synergy is missing two OpenSSH MAC names: `hmac-sha1-96-etm@openssh.com` and `hmac-md5-96-etm@openssh.com`.
- Public key / certificate algorithms: Partial parity. Synergy supports standard RSA/ECDSA/Ed25519 and OpenSSH cert forms, but does not expose OpenSSH security-key algorithms (`sk-*`) and related cert variants.
- Authentication methods: Partial parity. Synergy supports `publickey`, `password`, `keyboard-interactive`, but does not expose `hostbased` or `gssapi-with-mic` as built-in methods.
- OpenSSH protocol extensions: Good support (`zlib@openssh.com`, strict-KEX markers, hostkeys update/prove, streamlocal forwarding, agent-forward request).

Support level legend:
- Full: equivalent support is present
- Partial: some but not all support is present
- Missing: not present in current implementation
- Extended: Synergy offers additional capability not in baseline OpenSSH list

## Scope and Method
Data sources:
- OpenSSH capability baseline from local commands: `ssh -Q ...`, `ssh -G localhost`, and `sshd -T` (with a temporary host key).
- Synergy capability baseline from component registration and source:
  - ServiceLoader registration in core and BC modules
  - JPMS `module-info.java` provider lists
  - Algorithm constants and extension identifiers in `SshContext` and transport/protocol classes

This review separates client and server roles and notes where support depends on the optional `maverick-bc` module.

## Client Role Comparison

### 1) Transport Key Exchange (KEX)
OpenSSH baseline (observed):
- `curve25519-sha256`, `curve25519-sha256@libssh.org`
- `ecdh-sha2-nistp256`, `ecdh-sha2-nistp384`, `ecdh-sha2-nistp521`
- `diffie-hellman-group14-sha256`, `diffie-hellman-group16-sha512`, `diffie-hellman-group18-sha512`, `diffie-hellman-group-exchange-sha256`
- legacy also available (`group1`, `group14-sha1`, `gex-sha1`)
- PQ/hybrid: `sntrup761x25519-sha512`, `sntrup761x25519-sha512@openssh.com`, `mlkem768x25519-sha256`

Synergy client support:
- Core client module provides curve25519, nistp ECDH, DH groups, GEX, plus RSA KEX variants.
- BC module provides PQ/hybrid KEX including:
  - `sntrup761x25519-sha512`
  - `sntrup761x25519-sha512@openssh.com`
  - `mlkem768x25519-sha256`
  - `mlkem768nistp256-sha256`
  - `mlkem1024nistp384-sha384`

Support level: Full (OpenSSH parity), Extended (additional Synergy PQ hybrids).

### 2) Ciphers
OpenSSH baseline (observed):
- `aes128/256-gcm@openssh.com`, `chacha20-poly1305@openssh.com`
- `aes128/192/256-ctr`
- `aes128/192/256-cbc`, `3des-cbc`

Synergy client support:
- Includes all of the above names and additional legacy options (`3des-ctr`, blowfish, arcfour variants, etc.).

Support level: Full, Extended.

### 3) MACs
OpenSSH baseline (observed):
- `hmac-sha1`, `hmac-sha2-256`, `hmac-sha2-512`, `hmac-md5`
- ETM variants including `hmac-sha1-etm@openssh.com`, `hmac-sha2-256-etm@openssh.com`, `hmac-sha2-512-etm@openssh.com`, `hmac-md5-etm@openssh.com`
- also `hmac-sha1-96-etm@openssh.com`, `hmac-md5-96-etm@openssh.com`
- UMAC 64/128 and ETM forms

Synergy client support:
- Covers standard HMAC, ETM, and UMAC families.
- Missing OpenSSH names:
  - `hmac-sha1-96-etm@openssh.com`
  - `hmac-md5-96-etm@openssh.com`

Support level: Partial (high coverage, 2 OpenSSH MAC names missing), Extended (extra UMAC 32/96 variants).

### 4) Public Key and Certificate Algorithms
OpenSSH baseline (observed):
- Standard: Ed25519, ECDSA nistp256/384/521, RSA SHA-2
- Cert forms for OpenSSH key types
- Security key algorithms: `sk-ssh-ed25519@openssh.com`, `sk-ecdsa-sha2-nistp256@openssh.com` and cert variants

Synergy client support:
- Standard RSA/ECDSA/Ed25519 and OpenSSH cert classes are present.
- Also supports Ed448 (non-OpenSSH default).
- No `sk-*` security key algorithms found.

Support level: Partial, Extended (Ed448), Missing (`sk-*` family).

### 5) Authentication Methods
OpenSSH baseline capabilities/config fields include:
- `publickey`, `password`, `keyboard-interactive`, optional `hostbased`, optional `gssapi-with-mic`.

Synergy client support:
- Built-in authenticators for `publickey`, `password`, `keyboard-interactive`.
- No built-in `hostbased` or `gssapi-with-mic` path found.

Support level: Partial.

### 6) OpenSSH-specific Protocol Extensions (Client-side)
Detected support:
- Host key update/prove flow: `hostkeys-00@openssh.com`, `hostkeys-prove-00@openssh.com`
- Delayed compression: `zlib@openssh.com`
- Strict-KEX marker handling through transport-level strict extension identifiers
- Streamlocal forwarding types

Support level: Full for detected extension set.

## Server Role Comparison

### 1) Transport Key Exchange (KEX)
Synergy server mirrors core KEX parity with OpenSSH and adds BC-provided PQ/hybrid KEX when `maverick-bc` is present.

Support level: Full (OpenSSH parity), Extended (additional Synergy PQ hybrids).

### 2) Ciphers, MACs, Compression
Server uses the same component model as client for these families.

Support level:
- Ciphers: Full, Extended
- MACs: Partial (same two ETM-96 OpenSSH names missing)
- Compression: Full (`none` and `zlib@openssh.com` parity; Synergy also offers `zlib`)

### 3) Public Key and Certificates (Host/User Auth Algorithms)
Synergy server supports OpenSSH-style cert key classes and common key algorithms, but no `sk-*` family exposure found.

Support level: Partial (standard/cert strong coverage, missing `sk-*`).

### 4) Authentication Methods
Synergy server provides providers/verifiers for `publickey`, `password`, `keyboard-interactive`.
No built-in `hostbased`/`gssapi-with-mic` implementations found.

Support level: Partial.

### 5) OpenSSH-specific Protocol Extensions (Server-side)
Detected support:
- `hostkeys-prove-00@openssh.com`
- `zlib@openssh.com` delayed compression behavior
- strict-KEX markers in transport handling
- `direct-streamlocal@openssh.com` / `forwarded-streamlocal@openssh.com`
- `auth-agent-req@openssh.com`

Support level: Full for detected extension set.

## Specifications Missing or Partial (Prioritized)

### High priority gaps
1. OpenSSH security-key algorithms (`sk-*`) and cert variants
- Status: Missing on both client and server
- Impact: no native FIDO/U2F resident key interoperability at algorithm level

2. Authentication methods `hostbased` and `gssapi-with-mic`
- Status: Missing on both client and server
- Impact: environments standardized on host-based or Kerberos/GSSAPI auth need fallback methods

### Medium priority gaps
3. OpenSSH MAC names
- `hmac-sha1-96-etm@openssh.com`
- `hmac-md5-96-etm@openssh.com`
- Status: Missing on both client and server
- Impact: low for modern hardened configs, but can affect strict compatibility with legacy/explicit MAC policy sets

## Notes
- Synergy KEX parity with OpenSSH assumes `maverick-bc` is present for PQ/hybrid KEX algorithms.
- This review is based on the OpenSSH capability set available on the local host at review time.
