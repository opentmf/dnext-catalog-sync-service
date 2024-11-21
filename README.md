# Catalog Synchronization Service
This service synchronizes the catalog definitions under classpath:catalog folder.

## Execution Logic of the Service
For each Catalog Object under classpath:catalog/ folder:
- Retrieve the corresponding catalog object from the product catalog or the resource catalog.
- IF not found (404):
    - strip the following fields from the Catalog object:
        - href, revision, validFor, aclRelatedParty, lastUpdate, createdDate, updatedDate, createdBy, updatedBy, @schemaLocation
    - IF versioned Entity
        - create the catalog version="0", lifecycleStatus = "In design", validFor with endDateTime by sending a POST request
        - create the catalog version="1", lifecycleStatus = "Launched", validFor without an endDate by sending a POST request
    - ELSE IF soft versioned entity
        - create the catalog version="1", lifecycleStatus = "Launched", validFor without an endDate by sending a POST request
    - ELSE
        - create the catalog by sending a POST request
    - END
    - IF success
        - then continue with the next catalog object
    - ELSE,
        - FAIL: stop, do not continue
- ELSE IF Catalog object is patchable (only category is not patchable)
    - Strip the following fields from both requested and existing objects:
        - id, version, @baseType, @type, href, revision, validFor, aclRelatedParty, lastUpdate, createdDate, updatedDate, createdBy, updatedBy, @schemaLocation
    - compare the stripped json documents to check whether they are the same or not using the JSONCompareMode.NON_EXTENSIBLE mode, which means "not extensible, and non-strict array ordering".
    - IF they are the same
        - Then Success, just continue checking the next catalog object
    - ELSE
        - Patch the catalog object
        - IF Patch is successful
            - then continue with the next catalog object
        - ELSE
            - <span color:"Red">FAIL</span>, do not start the application.

## Using the Service
To use this service from a microservice, the following six small steps are necessary:

### 1. pom.xml Addition
Add this section:

```xml
<dependency>
  <groupId>com.pia.sh</groupId>
  <artifactId>sh-catalog-sync-service</artifactId>
</dependency>
```
### 2. Reorganize the Catalog files
In your microservice, the Catalog files must be under **classpath:catalog** folder with .json extension and the body of the catalog objects should be what product or resource catalog returns as a GET request.

Inside the catalog folder, the following sub folder structure should exist:
```text
catalog
  product
    bundles
    categories
    offerings
    specifications
  resource
    specifications
  service
    specifications
```
The following table is ordered by the synchronization and contains the descriptions of the above folders:

| Order | Folder                  | Contains                                    |
|:-----:|-------------------------|---------------------------------------------|
|   1   | resource/specifications | the resource specifications                 |
|   2   | service/specifications  | the service specifications                  |
|   3   | product/categories      | the category definitions                    |
|   4   | product/specifications  | the product specifications                  |
|   5   | product/offerings       | the non-bundle product offering definitions |
|   6   | product/bundles         | the bundle product offering definitions     |

**Note:** _Inner sub folders within the base sub folders are supported._

### 3. Specify the Catalog Sync Properties
In your application.yaml, specify the Catalog Sync Properties:
```yaml
pia:
  catalog-sync:
    enabled: true
    catalog-version: 1.0.0
    client: default
    product-catalog-url: http://dpc-api-svc/tmf-api/productCatalogManagement/v4
    resource-catalog-url: http://drc-api-svc/tmf-api/resourceCatalog/v4
    service-catalog-url: http://drc-api-svc/tmf-api/serviceCatalogManagement/v4

```
The Catalog Sync Service remembers the latest synchronized Catalog versions. If the specified catalogVersion is already the latest synchronized version, then no synchronization will take place. Therefore, it is the developers' responsibility to increase the version when any of the Catalog files change, to enforce the Catalog synchronization.

### 4. Disable JDBC Repositories
JDBC template is used only to obtain the DB connections by the db lock service and the rest is performed by pure JDBC calls by the DB Lock service. However, Spring Boot does not know this beforehand and checks if JDBC repositories can also be used as the repository implementations. In order to let Spring Boot know that we don't want to use JDBC repositories, the following should be added to application.yml file:

```yaml
spring:
  data:
    jdbc:
      repositories:
        enabled: false
```
### 5. Skip Catalog Sync in IT Tests
In order to skip the Catalog Sync in the IT tests, disable the Catalog Sync Service in your application-it.yml file:
```yaml
pia:
  db-lock:
    create-tables: false
  catalog-sync:
    enabled: false
```
## Sample Logs
Here are some sample log statements from a microservice's startup logs:

### Initial Synchronization of all Catalog Files

```text
47:21.583 INFO  [main] c.v.s.c.s.c.CatalogSyncAutoConfiguration -- Initializing the Catalog Sync Service.
47:21.876 DEBUG [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Attempting to acquire lock for lockType = CATALOG, and lockVersion = 1.0.0
47:21.879 DEBUG [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Acquired lock id = 5 for lockType = CATALOG, and lockVersion = 1.0.0.
47:21.882 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_CATEGORY id = 30
47:21.912 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_SPECIFICATION id = UCCPESpecification
47:21.925 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_SPECIFICATION id = UCDigitalLineAddOnLicenseSpecification
47:21.939 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_SPECIFICATION id = UCDigitalLineLicenseSpecification
47:21.952 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_SPECIFICATION id = UCMainNumberSpecification
47:21.965 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_SPECIFICATION id = UCNoNumberLicenseSpecification
47:21.980 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_SPECIFICATION id = UCNumberLicenseSpecification
47:21.994 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_SPECIFICATION id = UCPSTNClipPortInAddOnNumberSpecification
47:22.006 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_SPECIFICATION id = UCPSTNGeoNumberSpecification
47:22.020 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_SPECIFICATION id = UCPSTNNomadicNumberSpecification
47:22.033 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_SPECIFICATION id = UCPSTNNonGeoNumberSpecification
47:22.046 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_SPECIFICATION id = VBUCBackEndCompletionSpecification
47:22.102 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = CPEDevicesOffering
47:22.115 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCDigitalLineLicenseAddOnOffering
47:22.128 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCDigitalLineLicenseEntryOffering
47:22.141 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCDigitalLineLicensePremiumOffering
47:22.155 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCDigitalLineLicensePremiumVoiceOnlyOffering
47:22.168 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCDigitalLineLicenseStandardOffering
47:22.181 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCDigitalLineLicenseStandardVoiceOnlyOffering
47:22.195 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCMainNumberOffering
47:22.207 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCLiveReportsOffering
47:22.220 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCMeetingsAddOnLicenseOffering
47:22.232 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCRingCentralRoomsConnectorOffering
47:22.245 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCRingCentralRoomsOffering
47:22.259 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCDigitalLineBasicDomesticOffering
47:22.271 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCDomesticAdditionalLocalNumberOffering
47:22.284 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCDomesticAdditionalTollFreeNumberOffering
47:22.297 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = UCHotDeskingOffering
47:22.310 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = PSTNClipPortinNumberOffering
47:22.324 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = PSTNGeoNumberOffering
47:22.337 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = PSTNNomadicNumberOffering
47:22.350 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = PSTNNonGeoNumberOffering
47:22.364 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_OFFERING id = VBUCBackEndCompletionProductOffering
47:22.396 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_BUNDLES id = UCRingCentralOfficeLicenseEntryEdition
47:22.409 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_BUNDLES id = UCRingCentralOfficeLicensePremiumEdition
47:22.422 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_BUNDLES id = UCRingCentralOfficeLicensePremiumVoiceOnlyEdition
47:22.435 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_BUNDLES id = UCRingCentralOfficeLicenseStandardEdition
47:22.448 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create PRODUCT_BUNDLES id = UCRingCentralOfficeLicenseStandardVoiceOnlyEdition
47:22.469 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create RESOURCE_SPECIFICATION id = UCCLIPNumberResourceSpecification
47:22.482 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create RESOURCE_SPECIFICATION id = UCPORTINNumberResourceSpecification
47:22.495 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create RESOURCE_SPECIFICATION id = UCPSTNNumberResourceSpecification
47:22.515 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create SERVICE_SPECIFICATION id = gsipGeoNumberSS
47:22.528 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will create SERVICE_SPECIFICATION id = gsipNonGeoNumberSS
47:22.542 INFO  [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Catalog synchronization for version 1.0.0 has been completed. Updated Catalog count is 43 out of the total 43
47:22.543 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Deployed Catalogs and Their Versions follow:
47:22.543 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_CATEGORY id: 30 name: UC, version: 1.0, revision: 69
47:22.543 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_SPECIFICATION id: UCCPESpecification name: UCCPESpecification, version: null, revision: 0
47:22.543 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_SPECIFICATION id: UCDigitalLineAddOnLicenseSpecification name: UCDigitalLineAddOnLicenseSpecification, version: null, revision: 0
47:22.543 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_SPECIFICATION id: UCDigitalLineLicenseSpecification name: UCDigitalLineLicenseSpecification, version: null, revision: 0
47:22.543 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_SPECIFICATION id: UCMainNumberSpecification name: UCMainNumberSpecification, version: null, revision: 24
47:22.543 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_SPECIFICATION id: UCNoNumberLicenseSpecification name: UCNoNumberLicenseSpecification, version: null, revision: 0
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_SPECIFICATION id: UCNumberLicenseSpecification name: UCNumberLicenseSpecification, version: null, revision: 0
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_SPECIFICATION id: UCPSTNClipPortInAddOnNumberSpecification name: UCPSTNClipPortInAddOnNumberSpecification, version: null, revision: 24
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_SPECIFICATION id: UCPSTNGeoNumberSpecification name: UCPSTNGeoNumberSpecification, version: null, revision: 0
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_SPECIFICATION id: UCPSTNNomadicNumberSpecification name: UCPSTNNomadicNumberSpecification, version: null, revision: 0
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_SPECIFICATION id: UCPSTNNonGeoNumberSpecification name: UCPSTNNonGeoNumberSpecification, version: null, revision: 0
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_SPECIFICATION id: VBUCBackEndCompletionSpecification name: VBUCBackEndCompletionSpecification, version: null, revision: 1
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: CPEDevicesOffering name: CPE Devices, version: 1.0, revision: 0
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCDigitalLineLicenseAddOnOffering name: Office License Add-On, version: null, revision: 1
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCDigitalLineLicenseEntryOffering name: Office License - Entry Edition, version: 1.0, revision: 0
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCDigitalLineLicensePremiumOffering name: Office License - Premium Edition, version: 1.0, revision: 0
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCDigitalLineLicensePremiumVoiceOnlyOffering name: Office License - Premium Voice Only Edition, version: 1.0, revision: 0
47:22.544 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCDigitalLineLicenseStandardOffering name: Office License - Standard Edition, version: 1.0, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCDigitalLineLicenseStandardVoiceOnlyOffering name: Office License - Standard Voice Only Edition, version: 1.0, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCMainNumberOffering name: Main Number, version: 1.0, revision: 1
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCLiveReportsOffering name: Live Reports, version: 1.0, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCMeetingsAddOnLicenseOffering name: Meetings Add-On License, version: 1.0, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCRingCentralRoomsConnectorOffering name: RCV Rooms Connector, version: 1.0, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCRingCentralRoomsOffering name: RingCentral Rooms, version: 1.0, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCDigitalLineBasicDomesticOffering name: Limited Extension, version: null, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCDomesticAdditionalLocalNumberOffering name: Additional Phone Number, version: 1.0, revision: 3
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCDomesticAdditionalTollFreeNumberOffering name: Additional Phone Number - Toll Free, version: 1.0, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: UCHotDeskingOffering name: Hot Desking, version: null, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: PSTNClipPortinNumberOffering name: PSTN CLIP_PORTIN Number Add On, version: 1.0, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: PSTNGeoNumberOffering name: PSTN Geo Number, version: 1.0, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: PSTNNomadicNumberOffering name: PSTN Nomadic Number, version: 1.0, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: PSTNNonGeoNumberOffering name: PSTN Non Geo Number, version: 1.0, revision: 0
47:22.545 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_OFFERING id: VBUCBackEndCompletionProductOffering name: VBUC BackEnd Completion Product, version: 1.0, revision: 0
47:22.546 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_BUNDLES id: UCRingCentralOfficeLicenseEntryEdition name: Vodafone Business UC - Entry Edition, version: 1.0, revision: 22
47:22.546 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_BUNDLES id: UCRingCentralOfficeLicensePremiumEdition name: Vodafone Business UC - Premium Edition, version: 1.0, revision: 45
47:22.546 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_BUNDLES id: UCRingCentralOfficeLicensePremiumVoiceOnlyEdition name: Vodafone Business UC -Premium Edition - Voice only​, version: 1.0, revision: 30
47:22.546 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_BUNDLES id: UCRingCentralOfficeLicenseStandardEdition name: Vodafone Business UC - Standard Edition, version: 1.0, revision: 41
47:22.546 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: PRODUCT_BUNDLES id: UCRingCentralOfficeLicenseStandardVoiceOnlyEdition name: Vodafone Business UC -Standard -Voice Only, version: 1.0, revision: 25
47:22.546 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: RESOURCE_SPECIFICATION id: UCCLIPNumberResourceSpecification name: UCCLIPNumberResourceSpecification, version: null, revision: null
47:22.546 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: RESOURCE_SPECIFICATION id: UCPORTINNumberResourceSpecification name: UCPORTINNumberResourceSpecification, version: null, revision: null
47:22.546 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: RESOURCE_SPECIFICATION id: UCPSTNNumberResourceSpecification name: UCPSTNNumberResourceSpecification, version: null, revision: null
47:22.546 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: SERVICE_SPECIFICATION id: gsipGeoNumberSS name: GSIPGeoNumberSS, version: 0, revision: 0
47:22.547 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Created: SERVICE_SPECIFICATION id: gsipNonGeoNumberSS name: GSIPNonGeoNumberSS, version: 0, revision: 0
47:22.549 INFO  [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Released lock after 0 seconds. lockId = 5, and lockType = CATALOG
47:22.553 INFO  [main] c.v.s.c.s.c.CatalogSyncAutoConfiguration -- Completed initializing the Catalog Sync Service.
```
### When all Catalog files are up-to-date
```text
47:18.448 INFO  [main] c.v.s.c.s.c.CatalogSyncAutoConfiguration -- Initializing the Catalog Sync Service.
47:18.456 DEBUG [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Attempting to acquire lock for lockType = CATALOG, and lockVersion = 1.0.0
47:18.466 DEBUG [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Acquired lock id = 1 for lockType = CATALOG, and lockVersion = 1.0.0.
47:18.469 INFO  [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Released lock after 0 seconds. lockId = 1, and lockType = CATALOG
47:18.469 INFO  [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Catalog files are already up-to-date for version 1.0.0.
47:18.473 INFO  [main] c.v.s.c.s.c.CatalogSyncAutoConfiguration -- Completed initializing the Catalog Sync Service.
```
### When all files are up-to-date but version is increased
```text
47:22.581 INFO  [main] c.v.s.c.s.c.CatalogSyncAutoConfiguration -- Initializing the Catalog Sync Service.
47:22.721 DEBUG [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Attempting to acquire lock for lockType = CATALOG, and lockVersion = 1.0.0
47:22.722 DEBUG [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Acquired lock id = 7 for lockType = CATALOG, and lockVersion = 1.0.0.
47:22.724 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch checks for PRODUCT_CATEGORY, id = 30
47:22.739 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_SPECIFICATION id = UCCPESpecification is up to date.
47:22.741 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_SPECIFICATION id = UCDigitalLineAddOnLicenseSpecification is up to date.
47:22.743 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_SPECIFICATION id = UCDigitalLineLicenseSpecification is up to date.
47:22.744 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_SPECIFICATION id = UCMainNumberSpecification is up to date.
47:22.745 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_SPECIFICATION id = UCNoNumberLicenseSpecification is up to date.
47:22.746 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_SPECIFICATION id = UCNumberLicenseSpecification is up to date.
47:22.748 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_SPECIFICATION id = UCPSTNClipPortInAddOnNumberSpecification is up to date.
47:22.749 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_SPECIFICATION id = UCPSTNGeoNumberSpecification is up to date.
47:22.750 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_SPECIFICATION id = UCPSTNNomadicNumberSpecification is up to date.
47:22.751 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_SPECIFICATION id = UCPSTNNonGeoNumberSpecification is up to date.
47:22.752 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_SPECIFICATION id = VBUCBackEndCompletionSpecification is up to date.
47:22.782 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = CPEDevicesOffering is up to date.
47:22.783 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCDigitalLineLicenseAddOnOffering is up to date.
47:22.785 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCDigitalLineLicenseEntryOffering is up to date.
47:22.787 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCDigitalLineLicensePremiumOffering is up to date.
47:22.789 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCDigitalLineLicensePremiumVoiceOnlyOffering is up to date.
47:22.790 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCDigitalLineLicenseStandardOffering is up to date.
47:22.793 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCDigitalLineLicenseStandardVoiceOnlyOffering is up to date.
47:22.794 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCMainNumberOffering is up to date.
47:22.795 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCLiveReportsOffering is up to date.
47:22.797 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCMeetingsAddOnLicenseOffering is up to date.
47:22.798 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCRingCentralRoomsConnectorOffering is up to date.
47:22.799 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCRingCentralRoomsOffering is up to date.
47:22.801 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCDigitalLineBasicDomesticOffering is up to date.
47:22.802 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCDomesticAdditionalLocalNumberOffering is up to date.
47:22.803 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCDomesticAdditionalTollFreeNumberOffering is up to date.
47:22.804 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = UCHotDeskingOffering is up to date.
47:22.805 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = PSTNClipPortinNumberOffering is up to date.
47:22.807 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = PSTNGeoNumberOffering is up to date.
47:22.808 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = PSTNNomadicNumberOffering is up to date.
47:22.809 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = PSTNNonGeoNumberOffering is up to date.
47:22.810 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_OFFERING id = VBUCBackEndCompletionProductOffering is up to date.
47:22.820 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_BUNDLES id = UCRingCentralOfficeLicenseEntryEdition is up to date.
47:22.821 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_BUNDLES id = UCRingCentralOfficeLicensePremiumEdition is up to date.
47:22.823 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_BUNDLES id = UCRingCentralOfficeLicensePremiumVoiceOnlyEdition is up to date.
47:22.824 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_BUNDLES id = UCRingCentralOfficeLicenseStandardEdition is up to date.
47:22.826 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because PRODUCT_BUNDLES id = UCRingCentralOfficeLicenseStandardVoiceOnlyEdition is up to date.
47:22.834 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because RESOURCE_SPECIFICATION id = UCCLIPNumberResourceSpecification is up to date.
47:22.835 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because RESOURCE_SPECIFICATION id = UCPORTINNumberResourceSpecification is up to date.
47:22.837 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because RESOURCE_SPECIFICATION id = UCPSTNNumberResourceSpecification is up to date.
47:22.844 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because SERVICE_SPECIFICATION id = gsipGeoNumberSS is up to date.
47:22.845 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch because SERVICE_SPECIFICATION id = gsipNonGeoNumberSS is up to date.
47:22.848 WARN  [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Catalog synchronization completed without updating any Catalog. The specified catalogVersion was: 1.0.0. Hint: Do not change the catalogVersion when there are no Catalog changes.
47:22.851 INFO  [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Released lock after 0 seconds. lockId = 7, and lockType = CATALOG
47:23.118 INFO  [main] c.v.s.c.s.c.CatalogSyncAutoConfiguration -- Completed initializing the Catalog Sync Service.
```
### When some of the Catalog Files Have Changed
```text
47:19.851 INFO  [main] c.v.s.c.s.c.CatalogSyncAutoConfiguration -- Initializing the Catalog Sync Service.
47:20.438 DEBUG [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Attempting to acquire lock for lockType = CATALOG, and lockVersion = 1.0.0
47:20.442 DEBUG [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Acquired lock id = 3 for lockType = CATALOG, and lockVersion = 1.0.0.
47:20.448 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Skipping patch checks for PRODUCT_CATEGORY, id = 30
47:20.472 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_SPECIFICATION id = UCCPESpecification
47:20.502 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_SPECIFICATION id = UCDigitalLineAddOnLicenseSpecification
47:20.517 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_SPECIFICATION id = UCDigitalLineLicenseSpecification
47:20.532 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_SPECIFICATION id = UCMainNumberSpecification
47:20.547 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_SPECIFICATION id = UCNoNumberLicenseSpecification
47:20.561 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_SPECIFICATION id = UCNumberLicenseSpecification
47:20.577 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_SPECIFICATION id = UCPSTNClipPortInAddOnNumberSpecification
47:20.592 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_SPECIFICATION id = UCPSTNGeoNumberSpecification
47:20.606 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_SPECIFICATION id = UCPSTNNomadicNumberSpecification
47:20.622 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_SPECIFICATION id = UCPSTNNonGeoNumberSpecification
47:20.636 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_SPECIFICATION id = VBUCBackEndCompletionSpecification
47:20.718 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = CPEDevicesOffering
47:20.739 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCDigitalLineLicenseAddOnOffering
47:20.754 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCDigitalLineLicenseEntryOffering
47:20.769 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCDigitalLineLicensePremiumOffering
47:20.785 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCDigitalLineLicensePremiumVoiceOnlyOffering
47:20.799 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCDigitalLineLicenseStandardOffering
47:20.815 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCDigitalLineLicenseStandardVoiceOnlyOffering
47:20.830 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCMainNumberOffering
47:20.845 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCLiveReportsOffering
47:20.859 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCMeetingsAddOnLicenseOffering
47:20.873 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCRingCentralRoomsConnectorOffering
47:20.912 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCRingCentralRoomsOffering
47:20.926 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCDigitalLineBasicDomesticOffering
47:20.941 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCDomesticAdditionalLocalNumberOffering
47:20.955 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCDomesticAdditionalTollFreeNumberOffering
47:20.971 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = UCHotDeskingOffering
47:20.985 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = PSTNClipPortinNumberOffering
47:21.000 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = PSTNGeoNumberOffering
47:21.014 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = PSTNNomadicNumberOffering
47:21.028 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = PSTNNonGeoNumberOffering
47:21.042 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_OFFERING id = VBUCBackEndCompletionProductOffering
47:21.077 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_BUNDLES id = UCRingCentralOfficeLicenseEntryEdition
47:21.091 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_BUNDLES id = UCRingCentralOfficeLicensePremiumEdition
47:21.105 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_BUNDLES id = UCRingCentralOfficeLicensePremiumVoiceOnlyEdition
47:21.127 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_BUNDLES id = UCRingCentralOfficeLicenseStandardEdition
47:21.145 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch PRODUCT_BUNDLES id = UCRingCentralOfficeLicenseStandardVoiceOnlyEdition
47:21.184 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch RESOURCE_SPECIFICATION id = UCCLIPNumberResourceSpecification
47:21.199 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch RESOURCE_SPECIFICATION id = UCPORTINNumberResourceSpecification
47:21.214 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch RESOURCE_SPECIFICATION id = UCPSTNNumberResourceSpecification
47:21.238 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch SERVICE_SPECIFICATION id = gsipGeoNumberSS
47:21.252 DEBUG [reactor-http-epoll-3] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Will patch SERVICE_SPECIFICATION id = gsipNonGeoNumberSS
47:21.271 INFO  [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Catalog synchronization for version 1.0.0 has been completed. Updated Catalog count is 42 out of the total 43
47:21.271 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Deployed Catalogs and Their Versions follow:
47:21.272 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_SPECIFICATION id: UCCPESpecification name: UCCPESpecification, version: null, revision: 0
47:21.272 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_SPECIFICATION id: UCDigitalLineAddOnLicenseSpecification name: UCDigitalLineAddOnLicenseSpecification, version: null, revision: 0
47:21.272 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_SPECIFICATION id: UCDigitalLineLicenseSpecification name: UCDigitalLineLicenseSpecification, version: null, revision: 0
47:21.272 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_SPECIFICATION id: UCMainNumberSpecification name: UCMainNumberSpecification, version: null, revision: 24
47:21.272 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_SPECIFICATION id: UCNoNumberLicenseSpecification name: UCNoNumberLicenseSpecification, version: null, revision: 0
47:21.272 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_SPECIFICATION id: UCNumberLicenseSpecification name: UCNumberLicenseSpecification, version: null, revision: 0
47:21.273 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_SPECIFICATION id: UCPSTNClipPortInAddOnNumberSpecification name: UCPSTNClipPortInAddOnNumberSpecification, version: null, revision: 24
47:21.273 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_SPECIFICATION id: UCPSTNGeoNumberSpecification name: UCPSTNGeoNumberSpecification, version: null, revision: 0
47:21.273 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_SPECIFICATION id: UCPSTNNomadicNumberSpecification name: UCPSTNNomadicNumberSpecification, version: null, revision: 0
47:21.273 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_SPECIFICATION id: UCPSTNNonGeoNumberSpecification name: UCPSTNNonGeoNumberSpecification, version: null, revision: 0
47:21.273 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_SPECIFICATION id: VBUCBackEndCompletionSpecification name: VBUCBackEndCompletionSpecification, version: null, revision: 1
47:21.273 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: CPEDevicesOffering name: CPE Devices, version: 1.0, revision: 0
47:21.273 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCDigitalLineLicenseAddOnOffering name: Office License Add-On, version: null, revision: 1
47:21.273 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCDigitalLineLicenseEntryOffering name: Office License - Entry Edition, version: 1.0, revision: 0
47:21.273 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCDigitalLineLicensePremiumOffering name: Office License - Premium Edition, version: 1.0, revision: 0
47:21.273 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCDigitalLineLicensePremiumVoiceOnlyOffering name: Office License - Premium Voice Only Edition, version: 1.0, revision: 0
47:21.273 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCDigitalLineLicenseStandardOffering name: Office License - Standard Edition, version: 1.0, revision: 0
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCDigitalLineLicenseStandardVoiceOnlyOffering name: Office License - Standard Voice Only Edition, version: 1.0, revision: 0
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCMainNumberOffering name: Main Number, version: 1.0, revision: 1
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCLiveReportsOffering name: Live Reports, version: 1.0, revision: 0
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCMeetingsAddOnLicenseOffering name: Meetings Add-On License, version: 1.0, revision: 0
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCRingCentralRoomsConnectorOffering name: RCV Rooms Connector, version: 1.0, revision: 0
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCRingCentralRoomsOffering name: RingCentral Rooms, version: 1.0, revision: 0
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCDigitalLineBasicDomesticOffering name: Limited Extension, version: null, revision: 0
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCDomesticAdditionalLocalNumberOffering name: Additional Phone Number, version: 1.0, revision: 3
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCDomesticAdditionalTollFreeNumberOffering name: Additional Phone Number - Toll Free, version: 1.0, revision: 0
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: UCHotDeskingOffering name: Hot Desking, version: null, revision: 0
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: PSTNClipPortinNumberOffering name: PSTN CLIP_PORTIN Number Add On, version: 1.0, revision: 0
47:21.274 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: PSTNGeoNumberOffering name: PSTN Geo Number, version: 1.0, revision: 0
47:21.275 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: PSTNNomadicNumberOffering name: PSTN Nomadic Number, version: 1.0, revision: 0
47:21.275 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: PSTNNonGeoNumberOffering name: PSTN Non Geo Number, version: 1.0, revision: 0
47:21.275 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_OFFERING id: VBUCBackEndCompletionProductOffering name: VBUC BackEnd Completion Product, version: 1.0, revision: 0
47:21.275 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_BUNDLES id: UCRingCentralOfficeLicenseEntryEdition name: Vodafone Business UC - Entry Edition, version: 1.0, revision: 22
47:21.275 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_BUNDLES id: UCRingCentralOfficeLicensePremiumEdition name: Vodafone Business UC - Premium Edition, version: 1.0, revision: 45
47:21.275 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_BUNDLES id: UCRingCentralOfficeLicensePremiumVoiceOnlyEdition name: Vodafone Business UC -Premium Edition - Voice only​, version: 1.0, revision: 30
47:21.275 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_BUNDLES id: UCRingCentralOfficeLicenseStandardEdition name: Vodafone Business UC - Standard Edition, version: 1.0, revision: 41
47:21.276 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: PRODUCT_BUNDLES id: UCRingCentralOfficeLicenseStandardVoiceOnlyEdition name: Vodafone Business UC -Standard -Voice Only, version: 1.0, revision: 25
47:21.276 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: RESOURCE_SPECIFICATION id: UCCLIPNumberResourceSpecification name: UCCLIPNumberResourceSpecification, version: null, revision: null
47:21.276 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: RESOURCE_SPECIFICATION id: UCPORTINNumberResourceSpecification name: UCPORTINNumberResourceSpecification, version: null, revision: null
47:21.276 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: RESOURCE_SPECIFICATION id: UCPSTNNumberResourceSpecification name: UCPSTNNumberResourceSpecification, version: null, revision: null
47:21.276 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: SERVICE_SPECIFICATION id: gsipGeoNumberSS name: GSIPGeoNumberSS, version: 0, revision: 0
47:21.276 DEBUG [main] c.v.s.c.s.s.i.CatalogSyncServiceImpl -- Updated: SERVICE_SPECIFICATION id: gsipNonGeoNumberSS name: GSIPNonGeoNumberSS, version: 0, revision: 0
47:21.279 INFO  [main] c.v.s.d.l.s.i.DbLockServiceImpl -- Released lock after 0 seconds. lockId = 3, and lockType = CATALOG
47:21.532 INFO  [main] c.v.s.c.s.c.CatalogSyncAutoConfiguration -- Completed initializing the Catalog Sync Service.
```
## Version History
### 1.0.0
- Initial Version
### 1.0.1
- Updates to latest pia-web-clients and pia-db-lock-service
- Changes the catalog creation order to the following: Resource Spec > Service Spec -> Product Category, Spec, Offerings and Bundles
### 1.0.2
- Updates to latest pia-web-clients and pia-db-lock-service
### 1.0.3
- Updates to latest pia-web-clients and pia-db-lock-service
### 1.0.4
- Updates to the latest pia-db-lock-service 1.0.5
