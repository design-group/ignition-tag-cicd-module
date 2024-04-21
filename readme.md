# Tag CICD Module

This module is used to enable CICD practices for Ignition tags. It enables the capability to export and import tag configurations from a gateway to a git repository. This allows for the ability to track tag changes in a git repository and to be able to roll back changes if needed.

## Deterministic JSON Export

During the export process, the module creates a deterministically sorted copy of the JSON data to ensure that source control is not confused when a new file gets saved, and the keys are accidentally reordered. This process is applied recursively to all nested arrays and objects within the JSON data.

## Example Calls

A document of example API calls has been added, which can be found [here](docs/example-calls.md).

## API Endpoints

### Export Tags

`GET /data/tag-cicd/tags/export`

This endpoint exports the tag configuration as a JSON string.

#### Parameters

| Name | Type | Description |
| ---- | ---- | ----------- |
| `provider` | `string` | The tag provider to export from. If not specified, defaults to the `DEFAULT_PROVIDER`. |
| `baseTagPath` | `string` | The base tag path to start the export from. If not specified, defaults to an empty string, which means the export will start from the root of the tag provider. |
| `recursive` | `boolean` | If `true`, the export will recursively include all tags and folders under the `baseTagPath`. If `false`, only the direct children of the `baseTagPath` will be exported. |
| `localPropsOnly` | `boolean` | If `true`, only the local properties of the tags will be exported (user-created configuration). If `false`, all properties (including inherited properties) will be exported. |

#### Example Usage

```sh
curl "https://tag-cicd.localtest.me/data/tag-cicd/tags/export?provider=default&baseTagPath=MyTagFolder&recursive=true&localPropsOnly=true"
```

#### Response

The response will be a JSON string containing the exported tag configuration.

### Export Tags to File

`POST /data/tag-cicd/tags/export`

This endpoint exports the tag configuration and saves it to a specified file path on the Ignition gateway server.

#### Parameters

| Name | Type | Description |
| ---- | ---- | ----------- |
| `provider` | `string` | The tag provider to export from. If not specified, defaults to the `DEFAULT_PROVIDER`. |
| `baseTagPath` | `string` | The base tag path to start the export from. If not specified, defaults to an empty string, which means the export will start from the root of the tag provider. |
| `recursive` | `boolean` | If `true`, the export will recursively include all tags and folders under the `baseTagPath`. If `false`, only the direct children of the `baseTagPath` will be exported. |
| `localPropsOnly` | `boolean` | If `true`, only the local properties of the tags will be exported (user-created configuration). If `false`, all properties (including inherited properties) will be exported. |
| `filePath` | `string` | The file path on the Ignition gateway server where the exported tag configuration will be saved. This parameter is required. |
| `individualFilesPerObject` | `boolean` | If `true`, the export will create individual JSON files for each tag and folder, maintaining the tag hierarchy. If `false`, the entire tag configuration will be exported as a single JSON file. |
| `deleteExisting` | `boolean` | If `true`, any existing files or directories in the export directory that are not going to be replaced by the new export will be deleted before saving the new export. If `false` (default), existing files and directories will not be deleted. |

#### Example Usage

```sh
curl -X POST "http://tag-cicd.localtest.me/data/tag-cicd/tags/export?provider=default&baseTagPath=MyTagFolder&recursive=true&localPropsOnly=true&filePath=data/projects/my-project/tags.json&individualFilesPerObject=false&deleteExisting=true"
```

#### Response

The response will be a JSON string containing the exported tag configuration, similar to the `Export Tags` endpoint. The exported tag configuration will also be saved to the specified `filePath` on the Ignition gateway server.

### Import Tags

`POST /data/tag-cicd/tags/import`

This endpoint imports a tag configuration from a JSON string in the request body.

#### Parameters

| Name | Type | Description |
| ---- | ---- | ----------- |
| `provider` | `string` | The tag provider to import into. If not specified, it will be determined from the JSON payload. If the JSON payload does not specify a provider, the `DEFAULT_PROVIDER` will be used. |
| `baseTagPath` | `string` | The base tag path where the imported tags will be created/updated. If not specified, defaults to an empty string, which means the tags will be imported at the root of the tag provider. |
| `collisionPolicy` | `string` | The collision policy to use when importing tags. Valid values are: `"a"` (abort), `"o"` (overwrite), `"u"` (update), and `"d"` (delete and overwrite). If not specified, defaults to `"a"`. |
| `filePath` | `string` | The file path on the Ignition gateway server from where the tag configuration will be imported. This parameter is required when `individualFilesPerObject` is `true`. |
| `individualFilesPerObject` | `boolean` | If `true`, the import will expect individual JSON files for each tag and folder, maintaining the tag hierarchy. The `filePath` parameter should point to the directory containing the JSON files. If `false`, the import will expect a single JSON file containing the entire tag configuration. |

#### Example Usage

```sh
curl -X POST -H "Content-Type: application/json" -d @path/to/tag_configuration.json "http://tag-cicd.localtest.me/data/tag-cicd/tags/import?provider=default&baseTagPath=MyTagFolder&collisionPolicy=o"
```

#### Request Body

The request body should be a JSON string representing the tag configuration to import. The format of the JSON depends on the value of `individualFilesPerObject`:

- If `individualFilesPerObject` is `false`, the request body should be a single JSON object representing the entire tag configuration.
- If `individualFilesPerObject` is `true`, the request body should be omitted, and the tag configuration will be imported from individual JSON files located at the specified `filePath`.

#### Response

The response will be a JSON object containing the results of the import operation, including the created and deleted tags.

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

### Example Environment Setup

#### Leveraging SDKMAN

1. Install SDKMAN

```sh
curl -s "https://get.sdkman.io" | bash
```

2. Install Java

```sh
sdk install java java 17.0.11-zulu
```

3. Install Gradle

```sh
sdk install gradle 7.5.1
```

4. If you are going to deploy to a gateway with non-standard certificates installed, you will need to add the gateway's certificate to the Java truststore. This can be done by running the following commands:

```sh
keytool -import -cacerts -alias root_ca -file /path/to/root_ca.crt  -storepass changeit
keytool -import -cacerts -alias server_cert -file /path/to/server.crt -storepass changeit
```