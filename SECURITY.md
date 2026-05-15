# Security Policy

## Supported Versions

To see which versions of Apache Camel Karaf are supported please refer to this [page](https://camel.apache.org/download/).

## Reporting a Vulnerability

For information on how to report a new security problem please see [here](https://camel.apache.org/security/).

Apache Camel Karaf is a sub-project of Apache Camel and uses the same Apache
Camel / ASF private vulnerability reporting process. Do not open a public
issue, pull request, or mailing-list post about an unpublished vulnerability.

## Security Model

Before submitting a report, please read the project's
[Security Model](docs/modules/ROOT/pages/security-model.adoc). It documents who is
trusted, where the trust boundaries sit, and which classes the Camel PMC accepts as
a camel-karaf vulnerability versus what is operator responsibility (installing
features/bundles, exposing the Karaf shell, Pax-URL artifact integrity) or
out of scope.

camel-karaf is a runtime adapter: data-plane vulnerability classes
(deserialization, XXE, header injection, path traversal, SSRF, etc.) live in
Apache Camel core and components. The canonical model for those is the Apache
Camel [Security Model](https://github.com/apache/camel/blob/main/docs/user-manual/modules/ROOT/pages/security-model.adoc).
A defect in a packaged Camel component reached through a Karaf feature is an
Apache Camel report, not a camel-karaf one. Reports outside the documented
scope will be closed with a reference to the Security Model page.
