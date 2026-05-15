# Apache Camel Karaf - AI Agent Guidelines

Guidelines for AI agents working on this codebase.

## Project Info

Apache Camel Karaf provides Apache Karaf (OSGi) runtime support for Apache
Camel: Karaf feature descriptors that install Camel and its components as OSGi
bundles, an OSGi-aware core (`camel-core-osgi`), a Blueprint XML DSL
(`camel-blueprint`), and Karaf shell commands for inspecting and controlling
running Camel contexts and routes.

- Version: 4.18.2-SNAPSHOT
- Bundles Apache Camel: 4.18.1
- Built against Apache Karaf: 4.4.8 (minimum Karaf 4.4.6)
- Java: 17+
- Build: Maven

It is a sub-project of Apache Camel, under the same PMC and the same security
process.

## AI Agent Rules of Engagement

These rules apply to ALL AI agents working on this codebase.

### Attribution

- All AI-generated content (GitHub PR descriptions, review comments, issue
  comments) MUST clearly identify itself as AI-generated and mention the human
  operator. Example: "_Claude Code on behalf of [Human Name]_"

### PR Volume

- An agent MUST NOT open more than 10 PRs per day per operator to ensure human
  reviewers can keep up.
- Prioritize quality over quantity — fewer well-tested PRs are better than many
  shallow ones.

### Git branch

- An agent MUST NEVER push commits to a branch it did not create.
- If a contributor's PR needs changes, the agent may suggest changes via review
  comments, but must not push to their branch without explicit permission.
- An agent should prefer to use its own fork to push branches instead of the
  main `apache/camel-karaf` repository. It avoids filling the main repository
  with a long list of uncleaned branches.
- An agent must provide a useful name for the git branch. It should contain the
  global topic and issue number if possible.
- After a Pull Request is merged or rejected, the branch should be deleted.
- `main` requires **linear history** and PRs are **squash-merged** (see
  `.asf.yaml`). Keep branches rebased on `main`; do not add merge commits.

### GitHub Issue Ownership

camel-karaf tracks issues on **GitHub** (not JIRA):
<https://github.com/apache/camel-karaf/issues>.

- An agent MUST ONLY pick up issues that are **unassigned**.
- If an issue is already assigned to a human, the agent must not reassign it or
  work on it.
- Before starting work, the agent should comment on the issue stating it (and
  its operator) is taking it, so work is not duplicated.
- Reference the issue from the PR so it is linked and auto-closed on merge.

### PR Description Maintenance

When pushing new commits to a PR, **always update the PR description** (and
title if needed) to reflect the current state of the changeset. Use
`gh pr edit --title "..." --body "..."` after each push.

### PR Reviewers

When creating a PR, **always identify and request reviews** from the most
relevant committers:

- Run `git log --format='%an' --since='1 year' -- <affected-files> | sort | uniq -c | sort -rn | head -10`
  to find who has been most active on the affected files.
- Use `git blame` on key modified files to identify who wrote the code being
  changed.
- Cross-reference with the
  [committer list](https://home.apache.org/committers-by-project.html#camel)
  to ensure you request reviews from active committers.
- Request review from **at least 2 relevant committers** using
  `gh pr edit --add-reviewer`.
- When all comments are addressed and checks are green, re-request review so
  reviewers know the new changeset is ready.

### Merge Requirements

- An agent MUST NOT merge a PR if there are any **unresolved review
  conversations**.
- An agent MUST NOT merge a PR without at least **one human approval**.
- An agent MUST NOT approve its own PRs — human review is always required.

### Code Quality

- Every PR must include tests for new functionality or bug fixes.
- Every PR must include documentation updates where applicable.
- The build (`mvn clean install`) is the gate; it must pass before pushing.
  There is no separate enforced source-formatter plugin — match the style of
  the surrounding code and the Apache Camel conventions.
- Any generated files (catalog, features) must be regenerated and committed if
  the change affects them.

### Asynchronous Testing: Use Awaitility Instead of Thread.sleep

Do **NOT** use `Thread.sleep()` in test code. It leads to flaky, slow, and
non-deterministic tests. Use the
[Awaitility](https://github.com/awaitility/awaitility) library instead, which
is already available as a test dependency in this project.

```java
import static org.awaitility.Awaitility.await;

await().atMost(20, TimeUnit.SECONDS)
       .untilAsserted(() -> assertEquals(1, context.getRoutes().size()));
```

**Rules:**

- New test code MUST NOT introduce `Thread.sleep()` calls.
- When modifying existing test code that contains `Thread.sleep()`, migrate it
  to Awaitility.
- Always set an explicit `atMost` timeout to avoid hanging builds.
- OSGi/Karaf integration tests (Pax Exam) are inherently asynchronous; use
  Awaitility for service/route/context readiness rather than sleeping.

### Issue Investigation (Before Implementation)

Before implementing a fix, **thoroughly investigate** the issue's validity and
context. camel-karaf is a long-lived OSGi project — code often looks "wrong"
but exists for good OSGi reasons (classloader scoping, service dynamics,
Blueprint lifecycle). Do NOT jump straight to implementation.

**Required investigation steps:**

1. **Validate the issue**: Confirm the reported problem is real and
   reproducible. Question assumptions in the issue description.
2. **Check git history**: Run `git log --oneline <file>` and `git blame
   <file>`. Many fixes here are OSGi-dynamics workarounds (e.g.
   `OsgiTypeConverter` delegate invalidation, Blueprint proxy bean method
   resolution); understand *why* before changing.
3. **Search for related issues/PRs**: Look for prior discussion of the same
   OSGi behavior; the same area is often touched repeatedly.
4. **Distinguish camel-karaf vs Camel core**: If the defect is really in a
   packaged Camel component or in Camel core, the fix belongs in
   `apache/camel`, and camel-karaf only re-releases the upgraded version. See
   the Security Model below for the same split applied to vulnerabilities.
5. **Check if the "fix" reverts prior work**: If your change effectively
   reverts a prior intentional OSGi commit, stop and reconsider; if still
   justified, acknowledge it explicitly in the PR description.

**Present your findings** to the operator before implementing. Flag risks,
ambiguities, or cases where the issue may be invalid.

### Knowledge Cutoff Awareness

AI agents have a training-data cutoff and may not know about recent releases or
API changes in Camel, Karaf, OSGi, Pax Exam or Blueprint.

- When an issue, PR or code references a specific version of an external
  dependency, **verify it exists** (web search, Maven Central, release notes)
  before relying on or questioning it.
- Do not make authoritative claims about external project state based solely on
  training knowledge; verify the current state.

### Documentation Conventions

When writing or modifying `.adoc` documentation under `docs/`:

- **Use `xref:` for internal links**, not external `https://camel.apache.org/...`
  URLs, for pages inside this Antora module.
- Keep the user guide (`docs/modules/ROOT/pages/index.adoc`) and the navigation
  (`docs/modules/ROOT/nav.adoc`) in sync when adding pages.

## Security Model

camel-karaf has a documented threat model:
[`docs/modules/ROOT/pages/security-model.adoc`](docs/modules/ROOT/pages/security-model.adoc).
Use it when triaging security reports, deciding whether a finding warrants a
CVE, or reviewing a security-sensitive PR. For the **reporting** convention,
[`SECURITY.md`](SECURITY.md) at the repository root is the entry point. An
agent that discovers or is handed a suspected vulnerability MUST NOT open a
public issue, PR, or mailing-list post about it — follow the private Apache
Camel process and stop.

### The one thing to internalize

camel-karaf is a **runtime adapter, not a new data plane**. The data-plane
vulnerability classes (unsafe deserialization, XXE, expression/template
injection, path traversal, SSRF, Camel-header/bean-dispatch abuse, auth/authz
bypass, information disclosure, insecure defaults, back-end query injection)
live in **Apache Camel core and components**. Their canonical threat model is
the Apache Camel
[Security Model](https://github.com/apache/camel/blob/main/docs/user-manual/modules/ROOT/pages/security-model.adoc).
A defect in `camel-jackson` / `camel-xslt` / `camel-jms` (etc.) reached
through a Karaf feature is an **Apache Camel** report — route it there, do not
treat it as a camel-karaf finding. camel-karaf's own model covers only the
**Karaf-specific delta**.

### Trust assumptions (inherited from Apache Camel)

- **camel-karaf committers** are trusted to ship secure defaults and to not
  weaken a boundary Camel core holds.
- **Bundle and route authors** (including authors of **Blueprint XML**) are
  **fully trusted**. Blueprint XML is route-author code exactly as Java/XML/YAML
  DSL is in core; code execution by an author is by design, not a vulnerability.
- **Karaf operators/deployers** are **fully trusted**. Installing a
  feature/bundle, running the deployer, or using the Karaf shell are
  privileged code-deployment actions. Operator misconfiguration is not a
  camel-karaf vulnerability unless a camel-karaf default exposed it.
- **External message senders** are **untrusted** — the primary attacker model,
  but the code that must withstand them is overwhelmingly Camel core, not
  camel-karaf.

### Karaf-specific in scope

A report is in scope for **camel-karaf specifically** only when the defect is
in code this repo ships and is one of:

- The OSGi resolution layer (`camel-core-osgi`) **widening, for untrusted
  message data, a class/bean/component/language/dataformat sink that
  flat-classpath Camel core had closed**.
- A camel-karaf-authored class (`camel-core-osgi`, `camel-blueprint`, shell)
  being itself a primary deser/XXE/path-traversal/header-injection sink (not a
  passthrough to a Camel component).
- The Karaf shell or Blueprint integration **adding a capability beyond** what
  the DSL / JMX management surface already grants.

### Karaf-specific out of scope

- A data-plane defect in a **packaged Camel component** → it's an Apache Camel
  report; redirect, do not dismiss.
- Installing a hostile/vulnerable feature or bundle, or `wrap:`-ing an
  untrusted JAR — trusted operator action.
- **OSGi is not a security sandbox**; no isolation is promised between
  co-deployed bundles; camel-karaf does not rely on the (deprecated) OSGi
  `SecurityManager`.
- Pax-URL / Maven artifact-resolution integrity (repo TLS, checksum/PGP) —
  Karaf/Pax-URL/operator configuration.
- Exposing the Karaf shell/SSH on an untrusted network — it is a management
  surface, like camel-management/JMX in core.
- Build-time `tooling/` (feature/catalog/upgrade Maven plugins) and
  shipped-but-unsupported `tests/`, `tests/examples/`, `archetypes/`.
- Everything the Apache Camel Security Model places out of scope, applied to
  packaged components.

### Operator hardening (surface when reviewing a deployment)

- Keep the Karaf shell / SSH on loopback or a management network behind a
  strong JAAS realm.
- Restrict who may install features/bundles and run the deployer.
- Harden Pax-URL: HTTPS Maven repositories, checksum/PGP verification, avoid
  `wrap:`-ing untrusted JARs.
- Keep the bundled Camel patched (track camel-karaf releases following Camel
  security releases).
- Apply the full Apache Camel operator hardening checklist (security profile
  `prod`, vault secrets, JSSE TLS, strip `Camel*` headers at untrusted
  boundaries, no Java serialization on untrusted consumers).

### Committer review checklist (security-sensitive PRs)

When a PR touches `camel-core-osgi`, `camel-blueprint`, the shell, or the
feature descriptors:

- Does an `Osgi*Resolver` / `OsgiClassResolver` / `OsgiBeanRepository` change
  let a *name derived from message data* select a class/bean/component that
  flat-classpath Camel would not? That re-opens a Camel-core sink and **is** in
  scope.
- Does a Blueprint change cause **message data** (not bundle-author XML) to
  drive bean instantiation?
- Does a new/changed shell command add a capability beyond list / start / stop
  / suspend / resume / stats — i.e., make the shell more powerful than the
  JMX/DSL surface it mirrors?
- Does a feature descriptor change introduce a security-relevant default
  (auto-installed bundle, transport)? Defaults err toward least surprise.
- If the change is really fixing a Camel-core data-plane issue, it belongs in
  `apache/camel`; here, only the version upgrade.

## Structure

```
camel-karaf/
├── core/             # OSGi-aware Camel core (camel-core-osgi + repackaged core modules)
├── components/       # ~330 Camel components packaged for OSGi/Karaf (incl. camel-blueprint)
├── features/         # Karaf feature descriptors (camel-features.xml, spring-features.xml)
├── shell/            # Karaf shell commands (camel:context-*, camel:route-*, ...)
├── platforms/        # Karaf platform glue and command-core
├── catalog/          # Karaf-specific Camel catalog
├── tooling/          # Build-time Maven plugins + camel-upgrade tool (out of security model)
├── archetypes/       # camel-archetype-blueprint (sample scaffolding)
├── tests/            # Pax Exam OSGi integration tests, examples
└── docs/             # Antora AsciiDoc docs (incl. security-model.adoc)
```

## Build

```bash
mvn clean install                 # full build
mvn clean install -DskipTests     # skip tests
mvn clean install -pl shell -am   # single module with dependencies
```

OSGi integration tests use Pax Exam and are resource intensive; do not
parallelize Maven jobs.

### Upgrading the bundled Camel version

The `tooling/camel-upgrade` tool regenerates wrappers/features for a new Camel
release. Clone `apache/camel` next to this repo, check out the target
`camel-x.y.z` tag, build camel-karaf, then run the upgrade tool (see
`README.md`). Review the diff before opening a PR. The upgrade PR is the
mechanism by which Apache Camel security fixes reach camel-karaf.

## Conventions

OSGi-aware core classes:
- `Osgi<Name>` prefix in `org.apache.camel.karaf.core` (e.g. `OsgiClassResolver`,
  `OsgiComponentResolver`, `OsgiBeanRepository`).

Blueprint:
- `Camel<Name>FactoryBean` in `org.apache.camel.blueprint`; the namespace
  handler is `CamelNamespaceHandler`.

Shell commands:
- One class per command in `org.apache.camel.karaf.shell`, extending
  `CamelCommandSupport`, annotated `@Command(scope = "camel", name = "...")`
  and `@Service`.

Features:
- Declared in `features/src/main/feature/camel-features.xml`; one `<feature>`
  per Camel component, bundles referenced by `mvn:` (or `wrap:mvn:` for
  non-OSGi JARs).

Tests:
- `*Test.java` (JUnit 5); OSGi integration tests use Pax Exam under `tests/`.

## Commits

camel-karaf uses conventional-style subjects with the GitHub issue number:

```
fix(#NNN): Brief description
feat(#NNN): Brief description
ci: Brief description
```

PRs are squash-merged, so the squash subject gets a `(#PR)` suffix
automatically. Reference the GitHub issue so it auto-closes on merge.

## Links

- https://camel.apache.org/
- https://github.com/apache/camel-karaf
- https://github.com/apache/camel-karaf/issues
- https://camel.apache.org/security/
- dev@camel.apache.org
- https://camel.zulipchat.com/
