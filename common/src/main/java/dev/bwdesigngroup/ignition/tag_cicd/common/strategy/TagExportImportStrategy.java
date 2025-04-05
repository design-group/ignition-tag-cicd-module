package dev.bwdesigngroup.ignition.tag_cicd.common.strategy;

import dev.bwdesigngroup.ignition.tag_cicd.common.model.ExportMode;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

import java.io.IOException;

/**
 * Interface for tag export/import strategies.
 * Each implementation provides a specific way to export and import tags.
 * 
 * @author Keith Gamble
 */
public interface TagExportImportStrategy {

    /**
     * Export tags to disk.
     * 
     * @param tagManager            The GatewayTagManager instance
     * @param provider              The tag provider name
     * @param baseTagPath           The base tag path to export from
     * @param recursive             Whether to export tags recursively
     * @param localPropsOnly        Whether to export only local properties
     * @param filePath              The target file or directory path
     * @param deleteExisting        Whether to delete existing files before export
     * @param excludeUdtDefinitions Whether to exclude UDT definitions
     * @throws IOException If an error occurs during export
     */
    void exportTagsToDisk(
            GatewayTagManager tagManager,
            String provider,
            String baseTagPath,
            boolean recursive,
            boolean localPropsOnly,
            String filePath,
            boolean deleteExisting,
            boolean excludeUdtDefinitions) throws IOException;

    /**
     * Import tags from a source file or directory.
     * 
     * @param tagManager      The GatewayTagManager instance
     * @param provider        The target tag provider
     * @param baseTagPath     The base tag path to import to
     * @param sourcePath      The source file or directory path
     * @param collisionPolicy The collision policy to use
     * @return A JsonObject containing information about the imported tags
     * @throws IOException If an error occurs during import
     */
    JsonObject importTagsFromSource(
            GatewayTagManager tagManager,
            String provider,
            String baseTagPath,
            String sourcePath,
            String collisionPolicy) throws IOException;

    /**
     * Get the export mode associated with this strategy.
     * 
     * @return The export mode
     */
    ExportMode getExportMode();
}