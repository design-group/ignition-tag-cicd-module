package com.bwdesigngroup.ignition.tag_cicd.common;

public interface TagCICDRPC {
        String exportTags(String provider, String baseTagPath, String filePath, boolean recursive,
                        boolean localPropsOnly,
                        boolean individualFilesPerObject, boolean deleteExisting, boolean excludeUdtDefinitions);

        String importTags(String provider, String baseTagPath, String sourcePath, String collisionPolicy,
                        boolean individualFilesPerObject);

        String exportTagsFromConfig();

        String importTagsFromConfig();

        String getTagConfig();
}