<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->

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
