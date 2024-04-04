# Tag CICD Module

This module is used to enable CICD practices for Ignition tags. It enables the capability to export and import tag configurations from a gateway to a git repository. This allows for the ability to track tag changes in a git repository and to be able to roll back changes if needed.

## API Endpoints

### Export Tags To String

`GET /data/tag-cicd/tags/export`

#### Parameters

| Name | Type | Description |
| ---- | ---- | ----------- |
| `provider` | `string` | The tag provider to export from. |
| `baseTagPath` | `string` | The base tag path to start the export from. |
| `recursive` | `boolean` | If true, will recursively search the `baseTagPath` for tags. If false, will only search for the direct children of `baseTagPath` for tags. |
| `localPropsOnly` | `boolean` | Set to True to only return configuration created by a user (aka no inherited properties). Useful for tag export and tag UI edits of raw JSON text. |

#### Response

The response will be a `json` file containing the exported tags.

### Export Tags To File

`POST /data/tag-cicd/tags/export-file`

#### Parameters

| Name | Type | Description |
| ---- | ---- | ----------- |
| `provider` | `string` | The tag provider to export from. |
| `baseTagPath` | `string` | The base tag path to start the export from. |
| `recursive` | `boolean` | If true, will recursively search the `baseTagPath` for tags. If false, will only search for the direct children of `baseTagPath` for tags. |
| `localPropsOnly` | `boolean` | Set to True to only return configuration created by a user (aka no inherited properties). Useful for tag export and tag UI edits of raw JSON text. |
| `filePath` | `string` | The file path to export the tags to, local to the Ignition Gateway |
| `individualFilesPerObject` | `boolean` | If true, will export each tag to a separate file in a folder structure representative of the tag configuration model. If false, will export all tags to a single file. |

#### Response

The response will be the `json` string of the exported tags. This is the same as the `Export Tags To String` endpoint.

The file will be created at the `filePath` specified in the request.

### Import Tags

`POST /data/tag-cicd/tags/import`

#### Parameters

| Name | Type | Description |
| ---- | ---- | ----------- |
| `provider` | `string` | The tag provider to import to. |
| `baseTagPath` | `string` | The base tag path to start the import to. |
| `collisionPolicy` | `string` | The collision policy to use when importing tags. Possible values are `a`, `o`, `u`, `d`. `d` is added for this module, and its purpose is to do a hard switch of the tags being imported. It will delete any tags found underneath the `baseTagPath` that are not in your import file. |

#### Body

The body of the request should be a `json` file export of the tags you want to import. The format of the file should be the same as the export file from either the `Export Tags` endpoint or the `Export Tags` button in the Ignition Designer.

#### Response

The response will be a list of qualified values describing if each tag import was successful.

### How to use

1. Checkout Branch
2. Import Branch Tags:
   1. Replace `tag_configuration.json` with your exported tag file path (leave the `@` in front of the file name)
   2. Replace `<my_gateway_url>` with your gateway url

```sh
curl -X POST -H "Content-Type: application/json" -d @tag_configuration.json https://<my_gateway_url>/data/tag-cicd/tags/import\?collisionPolicy\=d
```

3. Make Changes
4. Export Branch Tags:
   1. Replace `<my_gateway_url>` with your gateway url

```sh
curl https://<my_gateway_url>/data/tag-cicd/tags/export\?recursive\=true -o tag_configuration.json
```
1. Add & Commit changes to branch
2. Push to repo

## Building the Module

Within the root directory there is a file named `gradle.properties.template`. This file should be copied to `gradle.properties` and the properties within it should be filled out with the appropriate values.

| Property | Description |
| -------- | ----------- |
| `ignition.signing.keystoreFile` | The path to the keystore file. |
| `ignition.signing.keystorePassword` | The password for the keystore. |
| `ignition.signing.certFile` | The path to the certificate file. |
| `ignition.signing.certAlias` | The alias of the certificate. |
| `ignition.signing.certPassword` | The password for the certificate. |

Once the `gradle.properties` file has been filled out, the module can be built by running the following command:

```sh
./gradlew build
```

### Environment Setup

#### Leveraging SDKMAN

1. Install SDKMAN

```sh
curl -s "https://get.sdkman.io" | bash
```

2. Install Java

```sh
sdk install java java 11.0.22-zulu
```

3. Install Gradle

```sh
sdk install gradle 6.8.2
```

4. If you are going to deploy to a gateway with non-standard certificates installed, you will need to add the gateway's certificate to the Java truststore. This can be done by running the following

```sh
keytool -import -alias root_ca -file /path/to/root_ca.crt -keystore ~/.sdkman/candidates/java/current/lib/security/cacertss -storepass changeit
keytool -import -alias server_cert -file /path/to/server.crt -keystore ~/.sdkman/candidates/java/current/lib/security/cacertss -storepass changeit
```