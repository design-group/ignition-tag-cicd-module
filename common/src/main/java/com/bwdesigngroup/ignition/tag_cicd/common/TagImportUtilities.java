/*
 * Copyright 2023 Barry-Wehmiller Design Group
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.bwdesigngroup.ignition.tag_cicd.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.stream.Stream;


/**
 *
 * @author Keith Gamble
 */
public class TagImportUtilities {
	private static final Logger logger = LoggerFactory.getLogger(TagImportUtilities.class.getName());

    public static JsonObject readTagsFromDirectory(String directoryPath) throws IOException {
		logger.trace("Reading tags from directory: " + directoryPath);
        Path path = Paths.get(directoryPath);
        return readTagsRecursively(path);
    }

    private static JsonObject readTagsRecursively(Path path) throws IOException {
        JsonObject folderObject = new JsonObject();
        JsonArray tagsArray = new JsonArray();

        try (Stream<Path> paths = Files.walk(path, 1)) {
            paths.filter(Files::isRegularFile)
                 .forEach(file -> {
                     try {
                         String content = new String(Files.readAllBytes(file));
                         JsonObject tagObject = TagUtilities.stringToJson(content).getAsJsonObject();
                         tagsArray.add(tagObject);
                     } catch (IOException e) {
						logger.error("Error reading file: " + file.toString(), e);
						e.printStackTrace();
                     }
                 });

            Files.list(path)
                 .filter(Files::isDirectory)
                 .forEach(subDir -> {
                     try {
                         if (!subDir.equals(path)) { // Avoid self-referencing
                             JsonObject subFolder = readTagsRecursively(subDir);
                             tagsArray.add(subFolder);
                         }
                     } catch (IOException e) {
                        logger.error("Error reading directory: " + subDir.toString(), e);
						e.printStackTrace();
                     }
                 });
        }

        folderObject.add("tags", tagsArray);
        folderObject.addProperty("name", path.getFileName().toString());
        folderObject.addProperty("tagType", Files.isDirectory(path) ? "Folder" : "Tag");
        return folderObject;
    }

	public static JsonObject findTypesFolder(JsonObject tagsJson) {
    JsonArray tags = tagsJson.getAsJsonArray("tags");
    for (JsonElement tag : tags) {
        JsonObject tagObject = tag.getAsJsonObject();
        if (tagObject.get("name").getAsString().equals("_types_") && tagObject.get("tagType").getAsString().equals("Folder")) {
            return tagObject;
        }
    }
    return null;
}
}
