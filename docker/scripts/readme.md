# Tag Versioning

- Tag versioning is accomplished by using the [Tag-CI-CD module](https://github.com/keith-gamble/ignition-tag-cicd-module). This can be used to avoid saving tags in the gateway backup, and instead save them in a git repository. Some helper scripts are provided to assist in the process.

## tag-import.sh

### Tag Import Description

- This script is used to import tags from the file system to a target Ignition Gateway.

#### Tag Import Usage

- Run the script with the following command:

  ```sh
  ./scripts/tag-import.sh
  ```

- The following flags are available:
    - `-p` or `--provider` The tag provider to import to. Defaults to `default`.
    - `-c` or `--policy` Set the tag import policy.
    - `-d` or `--directory` Specify the directory to import tags from.
    - `-a` or `--auto` Automatically import tags from all providers in the specified directory.
    - `--force` Force overwrite if collision policy is set to `o`.
    - `--debug` Enable debug mode.
    - `-h` or `--help` Display the help message.

## tag-export.sh

### Tag Export Description

- This script is used to export tags from a source Ignition Gateway to your file system.

#### Tag Export Usage

- Run the script with the following command:

  ```sh
  ./scripts/tag-export.sh
  ```

- The following flags are available:
    - `-h` or `--help`: Display the help message.
    - `-p` or `--provider` The tag provider to export from. Defaults to `default`.
    - `-t` or `--base-tag-path` The base tag path to export from. Defaults to `/`.
    - `-r` or `--recursive` Recursively export tags. Defaults to `true`.
    - `-f` or `--file-path` The file path to export the tags to.
    - `-h` or `--help` Display the help message.
    - `-d` or `--delete-existing` Delete the existing tags in the file path.
