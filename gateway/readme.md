# Gateway Scope - Ignition Tag CI/CD Module

This directory contains the gateway-specific code for the Ignition Tag CI/CD Module. The gateway scope is responsible for registering components with the Perspective system and handling any server-side logic related to these components.

## Key Files

- `ExampleComponentLibraryGatewayHook.java`: The main entry point for the gateway module.
- `TagDeleteRoutes.java`: Handles tag deletion operations.
- `TagExportRoutes.java`: Manages tag export functionality.
- `TagImportRoutes.java`: Handles tag import operations.

## ExampleComponentLibraryGatewayHook

This class extends `AbstractGatewayModuleHook`, the hook provided by the Ignition SDK that acts as the entrypoint for our Java code, and is responsible for the lifecycle management of our module in the gateway scope.

### Key Methods

1. `setup(GatewayContext context)`: 
   - Called before startup.
   - We store our `GatewayContext` for later use.

2. `startup(LicenseState activationState)`:
   - Called to initialize the module.

3. `shutdown()`:
   - Called when the module is being shut down.

4. `getMountedResourceFolder()`:
   - Specifies the folder in the module's gateway jar files that should be mounted at `/res/${module-id}/foldername`.

5. `mountRouteHandlers(RouteGroup routes)`
   - Called to mount custom route handlers.
   - In this case, we mount a route handler for importing, exporting, and deleting tags.

6. `getMountPathAlias()`:
   - Provides an alternate mounting path instead of the module ID.
   - We use a constant `MODULE_URL_ALIAS` for consistency.

7. `isFreeModule()`:
   - Indicates whether this is a "free" module (not participating in the licensing system).
   - We return `true` as this is a free module.

## TagDeleteRoutes

This class is responsible for handling tag deletion operations.

### Key Methods

1. `mountRoutes()`:
   - Mounts the route for tag deletion.
   - Uses DELETE HTTP method.

2. `deleteAllTagsInProvider(RequestContext requestContext, HttpServletResponse httpServletResponse)`:
   - Deletes all tags within a specified path.
   - Returns a JsonObject indicating the status of the deletion process.

## TagExportRoutes

This class manages the export of tag configurations.

### Key Methods

1. `mountRoutes()`:
   - Mounts routes for tag export operations.
   - Includes routes for exporting tag configurations as JSON and saving to disk.

2. `getTagConfigurationAsString(RequestContext requestContext, HttpServletResponse httpServletResponse)`:
   - Returns the tag configuration as a JSON object.

3. `saveTagConfigurationToDisc(RequestContext requestContext, HttpServletResponse httpServletResponse)`:
   - Exports tag configuration to a specified file path.
   - Supports individual file export for each tag object.

4. `saveJsonFiles(JsonObject json, String baseFilePath)`:
   - Recursive method to save JSON files for complex tag structures.

## TagImportRoutes

This class handles the import of tag configurations.

### Key Methods

1. `mountRoutes()`:
   - Mounts the route for tag import operations.
   - Uses POST HTTP method.

2. `importTagConfiguration(RequestContext requestContext, HttpServletResponse httpServletResponse)`:
   - Imports a tag configuration from a JSON string in the request body.
   - Supports importing from individual files or a single JSON object.

3. `importTags(String provider, String basePath, CollisionPolicy collisionPolicy, JsonObject createdTags, JsonObject tagsJson)`:
   - Helper method to import tags, handling UDT types and tag hierarchies.

## Error Handling

We include error logging in case the component registry is not available, which would prevent our components from functioning correctly.

## Best Practices

1. We use a logger (`slf4j.Logger`) for proper error and info logging.
2. We check for null references before using them to avoid null pointer exceptions.
3. We use constants for component IDs and module aliases to maintain consistency across the module.
