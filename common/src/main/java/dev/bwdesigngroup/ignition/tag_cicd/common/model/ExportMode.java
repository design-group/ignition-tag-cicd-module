package dev.bwdesigngroup.ignition.tag_cicd.common.model;

/**
 * Enumeration of supported export/import modes for tag configurations.
 * 
 * @author Keith Gamble
 */
public enum ExportMode {
    /**
     * Export/import all tags as a single JSON file
     */
    SINGLE_FILE("singleFile", "Single JSON File"),

    /**
     * Export/import each tag, folder, and UDT as individual JSON files in a
     * hierarchical folder structure
     */
    INDIVIDUAL_FILES("individualFiles", "Individual Files"),

    /**
     * Export/import using a folder structure with tags.json and udts.json files in
     * each folder
     */
    STRUCTURED_FILES("structuredByType", "Structured Files");

    private final String code;
    private final String displayName;

    ExportMode(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the ExportMode from a string code.
     * 
     * @param code the code to look up
     * @return the corresponding ExportMode, or INDIVIDUAL_FILES if not found
     */
    public static ExportMode fromCode(String code) {
        for (ExportMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return INDIVIDUAL_FILES; // Default to individual files for backward compatibility
    }
}