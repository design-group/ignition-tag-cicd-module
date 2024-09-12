# Common Scope - Ignition Tag CI/CD Module

This directory contains the common code for the Ignition Tag CI/CD Module. The common scope is responsible for shared logic and utilities used by both the gateway and designer scopes.

## Key Files

- `FileUtilities.java`: Provides utility methods for file operations.
- `TagConfigUtilities.java`: Contains utilities for tag configuration.
- `TagImportUtilities.java`: Provides utilities for importing tags.
- `WebUtilities.java`: Contains web-related utility methods.

### FileUtilities.java

This file provides utility methods for file operations, including:

- `sortJsonElementRecursively`: Sorts JSON elements recursively for deterministic output.
- `saveJsonToFile`: Saves a JSON object to a file with pretty printing.
- `readFileAsString`: Reads the contents of a file and returns it as a string.
- `findTypesFolder`: Locates the "_types_" folder within a given directory.
- `deleteExistingFiles`: Manages file deletion based on JSON content and individual file settings.

### TagConfigUtilities.java

This file contains utilities for tag configuration, including:

- `getTagConfigurationModel`: Retrieves tag configuration for a given provider and tag path.
- `deleteTagsInConfigurationModel`: Deletes tags in a given configuration model.
- `convertQualityCodesToArray`: Converts QualityCode objects to a JSON array.
- `addQualityCodesToJsonObject`: Adds quality codes to a JSON object.
- `sortTagsAndUdtTypes`: Sorts tags and UDT types based on dependencies.

### TagImportUtilities.java

This file provides utilities for importing tags, including:

- `readTagsFromDirectory`: Reads tags recursively from a directory.
- `findTypesFolder`: Locates the "_types_" folder within a JSON object.

### WebUtilities.java

This file contains web-related utility methods, including:

- `getBadRequestError`: Generates a JSON object for a 400 Bad Request error.
- `getInternalServerErrorResponse`: Generates a JSON object for a 500 Internal Server Error.

## Best Practices

1. We use a logger (`slf4j.Logger`) for proper error and info logging.
2. We check for null references before using them to avoid null pointer exceptions.
3. We use constants for component IDs and module aliases to maintain consistency across the module.
4. JSON operations are performed using the `com.inductiveautomation.ignition.common.gson` package.
5. File operations are wrapped in try-catch blocks to handle potential IOExceptions.
6. Recursive methods are used for handling nested tag structures and JSON elements.
7. We use type-safe collections and generics where applicable.

## Key Constants

- `DEFAULT_PROVIDER`: The default tag provider, set to "default".
- `UDT_TYPES_FOLDER`: The folder name for UDT types, set to "_types_".

## Error Handling

- Web-related errors are returned as JSON objects with appropriate HTTP status codes.
- File operation errors are logged using the SLF4J logger.
- Exceptions are caught and handled appropriately, with stack traces included in error responses when necessary.

## Threading

- Some operations, such as tag deletion, are performed asynchronously using Java's CompletableFuture API.

## Dependencies

This module relies on the following key dependencies:

- Inductive Automation Ignition SDK
- SLF4J for logging
- GSON for JSON processing
