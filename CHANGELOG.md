# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [2.0.3] - 2026-03-29

### Added
- `catalog` endpoint synchronization for product catalogs (`SINGLE_VERSIONED`).

## [2.0.2] - 2026-03-28

### Added
- `productOfferingPrice` endpoint synchronization for product offering prices (`MULTI_VERSIONED`).

## [2.0.1] - 2026-03-25

### Changed
- Upgraded `opentmf-http-clients` to 2.1.0 (`RestTemplateUtil` renamed to `SyncClientUtil`).

## [2.0.0] - 2026-03-25

### Added
- Dual HTTP client support: both reactive (WebClient) and synchronous (RestClient) transports via `opentmf-http-clients`.
- `CatalogReactiveClient` and `CatalogRestClient` interfaces replacing the single `CatalogClient`.
- `CatalogRestClientImpl` for synchronous RestClient-based catalog communication.
- `ReactiveCatalogSyncServiceImpl` and `RestCatalogSyncServiceImpl` as separate service implementations.
- MULTI_VERSIONED entity handling: version 0 detection triggers creation of launched version; PATCH requests use versioned URL pattern (`/endpoint/id:(version=N)`).
- `CatalogUtil.equals()` for JSON comparison (moved from deleted `JacksonUtil2`).
- `CatalogUtil.launchedVersion()` (renamed from `version1()`).
- `CatalogSyncRestIT` integration test for the REST client path.
- `CatalogSyncService` exposed as a `@Bean` for testability.

### Changed
- **BREAKING**: Upgraded to Spring Boot 4.0.4 and Jackson 3.x (`tools.jackson` packages).
- **BREAKING**: Configuration property `opentmf.catalog-sync.client` renamed to `opentmf.catalog-sync.client-ref`.
- **BREAKING**: Replaced `opentmf-openid-webclient-provider` and `opentmf-basic-webclient-provider` with `opentmf-http-clients`.
- **BREAKING**: Removed `spring-boot-starter-parent`; Spring Boot is now managed via `spring-boot-dependencies` BOM import in `<dependencyManagement>`. All plugin versions are explicitly declared.
- Upgraded `opentmf-commons` to 2.1.0, `opentmf-db-lock-service` to 2.0.0.
- Replaced `mockserver-netty` test dependency with `opentmf-mockserver` 2.1.1.
- `CatalogRestClientImpl` now uses Spring's `RestClient` (fluent API) instead of the deprecated `RestTemplate`; auto-configuration looks up `<client-ref>RestClient` bean.
- `CatalogClient` interface split into `CatalogReactiveClient` (Mono-returning) and `CatalogRestClient` (String-returning).
- `CatalogClientImpl` renamed to `CatalogReactiveClientImpl`.
- `CatalogSyncServiceImpl` split into `ReactiveCatalogSyncServiceImpl` (reactive pipeline) and `RestCatalogSyncServiceImpl` (imperative).
- `SingleContext` fields relaxed from `SortedMap` to `Map`; added `existingVersion` field.
- `CatalogUtil.stripForPost()` and `stripForPatch()` are now non-mutating (create copies).
- Auto-configuration uses `SmartInitializingSingleton` and dynamically detects reactive vs REST client from `client-ref` bean prefix.
- `CatalogSyncServiceIT` renamed to `CatalogSyncReactiveIT`.

### Removed
- `CatalogClient` unified interface (replaced by `CatalogReactiveClient` and `CatalogRestClient`).
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
