# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [2.0.0-SNAPSHOT]

### Added
- Dual HTTP client support: both reactive (WebClient) and synchronous (RestTemplate) transports via `opentmf-http-clients`.
- MULTI_VERSIONED entity handling: version 0 detection triggers creation of launched version; PATCH requests use versioned URL pattern (`/endpoint/id:(version=N)`).
- `RestCatalogClientImpl` for synchronous RestTemplate-based catalog synchronisation.
- `CatalogUtil.equals()` for JSON comparison (moved from deleted `JacksonUtil2`).
- `CatalogUtil.launchedVersion()` (renamed from `version1()`).

### Changed
- **BREAKING**: Upgraded to Spring Boot 4.0.4 and Jackson 3.x (`tools.jackson` packages).
- **BREAKING**: Configuration property `opentmf.catalog-sync.client` renamed to `opentmf.catalog-sync.client-ref`.
- **BREAKING**: Replaced `opentmf-openid-webclient-provider` and `opentmf-basic-webclient-provider` with `opentmf-http-clients`.
- Upgraded `opentmf-commons` to 2.1.0, `opentmf-db-lock-service` to 2.0.0.
- Replaced `mockserver-netty` test dependency with `opentmf-mockserver` 2.1.1-SNAPSHOT.
- `CatalogClient` interface simplified to `get(URI)`, `post(URI, body)`, `patch(URI, mediaType, body)`.
- `CatalogClientImpl` renamed to `ReactiveCatalogClientImpl`.
- `SingleContext` fields relaxed from `SortedMap` to `Map`; added `existingVersion` field.
- `CatalogUtil.stripForPost()` and `stripForPatch()` are now non-mutating (create copies).
- Auto-configuration dynamically detects reactive vs REST client from `client-ref` bean prefix.

### Removed
- `JacksonUtil2` (replaced by `JacksonUtil` from `opentmf-commons` and `CatalogUtil`).
- `OpenidAuthClientsConfig` test configuration.
- Direct dependency on `mockserver-netty`.

## [1.1.1] - 2026-03-18

### Added
- Resource specification endpoint resolution by `@type`: `PhysicalResourceSpecification` and `LogicalResourceSpecification` use their respective backend paths; fallback to `resourceSpecification` when `@type` is missing or unknown.
- CHANGELOG.md with version history (Added / Changed / Fixed sections).

### Changed
- Maven plugins (source, javadoc, gpg, central-publishing) moved into a `release` profile; use `-P release` for publishing builds.

## [1.1.0]

### Fixed
- Exception constructor parameters.

## [1.0.9]

### Added
- Initial Open Source Version.

## [1.0.8]

### Added
- Web Client Starters to the autoconfiguration afterName.

## [1.0.7]

### Changed
- pia-web-clients to 1.0.8, for fewer dependencies for the reactive WebClient.
- Spring Boot to 3.4.1.

## [1.0.6]

### Changed
- pia-db-lock-service to 1.0.7.

### Fixed
- Autoconfiguration dependencies:
  - Configuration when property `pia.catalog-sync.enabled` is missing (default value is true).
  - Optional dependent web client configurations in afterName section to avoid NoClassDefFoundError when a web client provider is not on the classpath.

## [1.0.5]

### Changed
- pia-db-lock-service to 1.0.6.
- Spring Boot to 3.4.0.
- pia-web-clients to 1.0.7.

## [1.0.4]

### Changed
- pia-db-lock-service to latest 1.0.5.

## [1.0.3]

### Changed
- pia-web-clients and pia-db-lock-service to latest.

## [1.0.2]

### Changed
- pia-web-clients and pia-db-lock-service to latest.

## [1.0.1]

### Changed
- pia-web-clients and pia-db-lock-service to latest.
- Catalog creation order to: Resource Spec → Service Spec → Product Category, Spec, Offerings and Bundles.

## [1.0.0]

### Added
- Initial version.
