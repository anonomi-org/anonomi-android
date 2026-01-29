# Anonomi

**Decentralized messaging for high-threat environments.**  
Online over Tor. Fully functional offline. No servers. No trackers.

Anonomi is a privacy-first communication tool designed for situations where
networks are monitored, infrastructure is hostile, and trust assumptions fail.
It is built for people who cannot afford to be naive about surveillance,
centralization, or metadata.

####
⬇️ **Download the latest release (APK):**  
https://github.com/anonomi-org/anonomi-android/releases/latest

🔐 **Verify checksums & signatures:**  
See the release notes for verification instructions.

---

## What Anonomi is

Anonomi is an **Android-only**, decentralized communications suite that provides:

- **Secure messaging**  
  Peer-to-peer communication with no central servers to seize or subpoena.

- **Tor support (online mode)**  
  Route traffic through Tor, with bridge support when Tor is blocked.

- **Offline operation**  
  Communicate without the internet using:
    - Wi-Fi
    - Bluetooth
    - External storage (store-and-forward)

- **Private payments (Monero)**  
  In-chat payment requests using unique subaddresses to avoid persistent identifiers.

- **Offline maps & location sharing**  
  Share locations and navigate using imported map tiles — no external map servers.

Anonomi is designed to **reduce metadata exposure**, not to promise magical or absolute anonymity.

---

## Threat model (high level)

Anonomi assumes that, in serious cases:

- Network traffic may be monitored
- Blocking, throttling, and manipulation may occur
- Contacts may be compromised over time
- Physical device inspection is a realistic risk

Design choices prioritize:
- No centralized infrastructure
- Offline fallback paths
- Metadata discipline
- Pragmatic safety tools for coercive environments

A full threat model is documented separately.

---

## Project philosophy

Anonomi is:

- **Anti-authoritarian by design**  
  Incompatible with centralized control, mass surveillance, and coercive platforms.

- **Open and auditable**  
  Security through transparency, not marketing claims.

- **Community-governed**  
  No sponsors, no investors, no institutional control.

- **Honest about limits**  
  Trade-offs are documented. Assumptions are explicit.

📜 **Read the full manifesto:**  
➡️ [MANIFESTO.md](./MANIFESTO.md)

---

## Platform support

- ✅ **Android** — supported
- ❌ **iOS** — intentionally not supported

iOS imposes architectural and policy constraints that prevent the level of
network control, offline operation, and user agency required by Anonomi’s
threat model. Supporting iOS would require compromises this project is not
willing to make.

---

## Installation & releases

Anonomi is distributed **outside centralized app stores**.

- Signed APKs
- Published checksums
- Verifiable releases

Official releases are published via GitHub.

➡️ See releases and verification instructions in the documentation.

### About centralized app stores

Anonomi’s threat model assumes that distribution channels themselves can
leak metadata and impose constraints that are incompatible with high-risk use.

Centralized app stores, including Google Play, introduce risks such as:

- **Mandatory platform services**  
  Many Android devices require Google Play Services, increasing exposure to
  device-level identifiers and behavioral signals.

- **Automated scanning and policy enforcement**  
  Apps may be statically or dynamically analyzed, and removed or restricted
  without notice.

- **Metadata exposure**  
  Install timing, update behavior, IP-level correlation, and account linkage
  may be observable at the distribution layer.

- **Policy constraints on security tools**  
  Applications that include strong encryption, Tor integration, or mechanisms
  designed to bypass network blocking may face additional scrutiny or removal.

### For these reasons, Anonomi prioritizes direct APK distribution and F-Droid (soon), where users can independently verify builds and minimize reliance on centralized infrastructure.

---

## Open source & verification

Anonomi is developed transparently and in public.

Security relies on:
- Public, auditable source code
- Independent review
- The ability to fork, reproduce, and verify builds

---

## Contributing

Anonomi is under active development, with a strong focus on security,
resilience, and real-world adversarial environments.

Contributions of all kinds are welcome:

- Android development
- Networking & transport layers
- Cryptography review
- UX for high-risk users
- Documentation & threat analysis

➡️ See [CONTRIBUTING.md](./CONTRIBUTING.md)

---

## Contact

For security-related matters, responsible disclosure, or project coordination,
you can contact the Anonomi maintainers at:

**anonchatsec@protonmail.com**

Proton Mail access:
- Clearnet: https://proton.me
- Tor (official Proton Mail Tor entry): 
https://protonmailrmez3lotccipshtkleegetolb73fuirgj7r4o4vfu7ozyd.onion/

Please avoid sharing sensitive operational details unless strictly necessary,
and prefer encrypted communication when appropriate.

---

## Support & sustainability

Anonomi is independent and community-supported.

Support may include:
- Code contributions
- Infrastructure
- Testing devices
- Financial support

Financial contributions are accepted in a privacy-respecting way:

**Monero (XMR) — preferred**

`85kkMcowoNwQwji3ugetQvfwismWuvGWWLWfhWoyLjqnDAcgsnpVMsWG76zMm3zEb9WfUcJqBCKZKQ8wVox58tfr7dY7CXF`

**Bitcoin (BTC) — accepted (public by default)**

`bc1qqjw2qyj276jkwdd0wxm4y4vyggsmr73y9nm066`

For QR codes, and updated donation information, see:
https://anonomi.org/#supporters
(onion mirror soon available)

Part of the project’s resources are used to support the Tor ecosystem
by running relays and bridges.

**Sponsorship does not equal influence.**

---

## License

Anonomi is free and open source software, licensed under the
**GNU General Public License v3.0 (GPL-3.0)**.

This means you are free to:
- Use the software for any purpose
- Study how it works and modify it
- Redistribute it
- Distribute modified versions

Under the following conditions:
- Source code (including modifications) must be made available under the same license
- License notices and attributions must be preserved
- No additional restrictions may be imposed on users’ freedoms

Anonomi derives from the Briar project and other GPL-licensed components.
All required attributions and license notices are included in this repository.

See the [LICENSE](./LICENSE) file for full details.

---

## Disclaimer

Anonomi is designed for real-world adversarial environments, but no software
can eliminate all risk.

Users are responsible for understanding their own threat model,
operational security, and legal context.

Privacy is a condition you defend — not a checkbox you enable.