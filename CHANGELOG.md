# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

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
