# Contributing to Anonomi

Anonomi is built for hostile conditions and real-world risk.
Contributions are welcome — but they must respect the project’s threat model,
philosophy, and design constraints.

This is not a generic messaging app.
Convenience never outweighs safety.

---

## Guiding principles

All contributions must align with these principles:

- **No centralization**
    - No servers
    - No telemetry
    - No silent dependencies

- **Threat-aware by default**
    - Assume surveillance
    - Assume blocking
    - Assume device compromise is possible

- **Offline matters**
    - Features should degrade gracefully when the internet is unavailable or unsafe

- **No dark patterns**
    - No tracking
    - No growth hacks
    - No engagement metrics

If a contribution weakens these principles, it will not be accepted.

---

## Ways to contribute

### 🧑‍💻 Development
- Android (Kotlin / Java)
- Networking & transport layers
- Offline synchronization mechanisms
- Cryptographic correctness & review

### 🧪 Testing & review
- Threat model review
- Adversarial testing
- Metadata analysis
- Performance under constrained conditions

### 🧭 UX & documentation
- High-risk UX design
- Documentation clarity
- Operational security guidance
- Scenario-based explanations

### 📝 Writing
- Documentation
- Threat modeling
- Design rationale
- Security assumptions

---

## Before you start

Please read:

- `MANIFESTO.md`
- The threat model documentation
- Existing design discussions (issues / docs)

Understanding **why** things are done a certain way matters more than
implementing features quickly.

---

## Contribution process

1. **Open an issue first**  
   Especially for non-trivial changes. Explain:
    - What problem you’re solving
    - Why it matters in high-risk environments
    - Trade-offs involved

2. **Fork the repository**

3. **Create a focused branch**
    - One concern per branch
    - Avoid mixed refactors + features

4. **Make your changes**
    - Keep commits clean and descriptive
    - Avoid unnecessary dependencies

5. **Open a pull request**
    - Explain design decisions
    - Call out risks and limitations
    - Reference related issues

---

## What will be rejected

The following will **not** be accepted:

- Analytics, telemetry, or tracking
- Centralized services or accounts
- Features that require trust in third parties
- UX changes that prioritize growth over safety
- “Just works” abstractions that hide risk from users
- iOS ports or iOS-specific workarounds

---

## Security issues

**Do not open public issues for sensitive vulnerabilities.**

If you discover a security issue:
- Contact the maintainers privately
- Provide reproduction steps if safe
- Avoid public disclosure until mitigated

---

## Code of conduct

Anonomi is anti-authoritarian and anti-harassment.

We expect:
- Respectful technical discussion
- Good-faith disagreement
- No discrimination, harassment, or intimidation

Abusive behavior will result in exclusion from the project.

---

## Final note

Anonomi exists because some people cannot afford to lose private communication.

If you contribute, you are contributing to a tool that may be used under
real pressure — not a demo, not a startup pitch, not a toy.

Design accordingly.