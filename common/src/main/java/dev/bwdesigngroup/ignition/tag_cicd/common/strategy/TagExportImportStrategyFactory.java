package dev.bwdesigngroup.ignition.tag_cicd.common.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.bwdesigngroup.ignition.tag_cicd.common.model.ExportMode;

/**
 * Factory for creating TagExportImportStrategy instances based on ExportMode.
 * 
 * @author Keith Gamble
 */
public class TagExportImportStrategyFactory {
    private static final Logger logger = LoggerFactory.getLogger(TagExportImportStrategyFactory.class.getName());

    // Singleton pattern
    private static TagExportImportStrategyFactory instance;

    private TagExportImportStrategyFactory() {
        // Private constructor for singleton
    }

    /**
     * Get the singleton instance of the factory.
     * 
     * @return The singleton instance
     */
    public static synchronized TagExportImportStrategyFactory getInstance() {
        if (instance == null) {
            instance = new TagExportImportStrategyFactory();
        }
        return instance;
    }

    /**
     * Get the appropriate strategy for the given export mode.
     * 
     * @param mode The export mode
     * @return The corresponding strategy
     */
    public TagExportImportStrategy getStrategy(ExportMode mode) {
        logger.debug("Getting strategy for export mode: {}", mode.getDisplayName());

        switch (mode) {
            case SINGLE_FILE:
                return new SingleFileExportStrategy();
            case STRUCTURED_FILES:
                return new StructuredFilesExportStrategy();
            case INDIVIDUAL_FILES:
            default:
                return new IndividualFilesExportStrategy();
        }
    }

    /**
     * Get the appropriate strategy from a mode string.
     * 
     * @param modeCode The export mode code
     * @return The corresponding strategy
     */
    public TagExportImportStrategy getStrategy(String modeCode) {
        return getStrategy(ExportMode.fromCode(modeCode));
    }
}