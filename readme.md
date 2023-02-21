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
curl -X POST -H "Content-Type: application/json" -d @tag_configuration.json http://<my_gateway_url>/data/tag-cicd/tags/import\?collisionPolicy\=d
```

3. Make Changes
4. Export Branch Tags:
   1. Replace `<my_gateway_url>` with your gateway url

```sh
curl http://<my_gateway_url>/data/tag-cicd/tags/export\?recursive\=true -o tag_configuration.json
```

5. Add & Commit changes to branch
6. Push to repo