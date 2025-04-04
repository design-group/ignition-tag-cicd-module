package dev.kgamble.ignition.tag_cicd.common;

public interface TagCICDRPC {
        String exportTags(String provider, String baseTagPath, String filePath, boolean recursive,
                        boolean localPropsOnly, String exportMode, boolean deleteExisting,
                        boolean excludeUdtDefinitions);

        String importTags(String provider, String baseTagPath, String sourcePath, String collisionPolicy,
                        String exportMode);

        String exportTagsFromConfig();

        String importTagsFromConfig();

        String getTagConfig();

        String getExportModes();

        String getTagProviders();

        String saveTagConfig(String configJson);

        String getInstallDirectory();
}