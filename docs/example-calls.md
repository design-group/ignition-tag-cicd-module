# Single file split by type example

Individual file exports can be useful if you want to version control UDTs separately from tags, the following examples demonstrate how to export and import UDTs and tags separately.

### Export UDTs

To export all UDTs from a provider named 'default' and save them to a file named '_types_.json' in the 'data/projects/test/tags' directory, run the following command:

```sh
curl -X POST https://tag-cicd.localtest.me/data/tag-cicd/tags/export\?provider\=default\&baseTagPath\=_types_\&recursive\=true\&localPropsOnly\=true\&filePath\=data/projects/test/tags/_types_.json
```

### Export Tags

To export all tags from a provider named 'default' and save them to a file named 'Exchange.json' in the 'data/projects/test/tags' directory, run the following command:

```sh
curl -X POST https://tag-cicd.localtest.me/data/tag-cicd/tags/export\?provider\=default\&baseTagPath\=Exchange\&recursive\=true\&localPropsOnly\=true\&filePath\=data/projects/test/tags/Exchange.json
```

### Import UDTs

To import UDTs from a file named '_types_.json' in the 'data/projects/test/tags' directory, run the following command:

```sh
curl -X POST -H "Content-Type: application/json" -d @temp/docker/ignition-data/projects/test/tags/_types_.json https://tag-cicd.localtest.me/data/tag-cicd/tags/import\?provider\=default\&baseTagPath\=\&collisionPolicy\=o\&importType\=json
```

### Import Tags

To import tags from a file named 'Exchange.json' in the 'data/projects/test/tags' directory, run the following command:

```sh
curl -X POST -H "Content-Type: application/json" -d @temp/docker/ignition-data/projects/test/tags/Exchange.json https://tag-cicd.localtest.me/data/tag-cicd/tags/import\?provider\=default\&baseTagPath\=\&collisionPolicy\=o\&importType\=json
```



# Single File All

A single file export can be useful in a smaller project, that doesnt have a lot of tags. However can become cumbersome in larger projects. The following examples demonstrate how to export and import all tags in a single file.


### Export All Tags

To export all tags from a provider named 'default' and save them to a file named 'single.json' in the 'data/projects/test/tags' directory, run the following command:

```sh
curl -X POST https://tag-cicd.localtest.me/data/tag-cicd/tags/export\?provider\=default\&baseTagPath\=\&recursive\=true\&localPropsOnly\=true\&filePath\=data/projects/test/tags/single.json
```

### Import All Tags

To import tags from a file named 'single.json' in the 'data/projects/test/tags' directory, run the following command:

```sh
curl -X POST -H "Content-Type: application/json" -d @temp/docker/ignition-data/projects/test/tags/single.json https://tag-cicd.localtest.me/data/tag-cicd/tags/import\?provider\=default\&baseTagPath\=\&collisionPolicy\=o\&importType\=json
```

# Multi File Example

Multi-File exports can be useful in larger projects, where a single file export would be too large to manage. The following examples demonstrate how to export and import all tags in multiple files.

### Export All

To export all tags from a provider named 'default' and save them to individual files in the 'data/projects/test/tags/multi-tags' directory, run the following command:

```sh
curl -X POST https://tag-cicd.localtest.me/data/tag-cicd/tags/export\?provider\=default\&baseTagPath\=\&recursive\=true\&individualFilesPerObject\=true\&localPropsOnly\=true\&filePath\=data/projects/test/tags/multi-tags
```

### Import All

To import tags from the 'data/projects/test/tags/multi-tags' directory, run the following command:

```sh
curl -X POST https://tag-cicd.localtest.me/data/tag-cicd/tags/import\?provider\=default\&baseTagPath\=\&collisionPolicy\=o\&recursive\=true\&individualFilesPerObject\=true\&filePath\=data/projects/test/tags/multi-tags/
```

