/*
 * Copyright 2023 Barry-Wehmiller Design Group
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package dev.bwdesigngroup.ignition.tag_cicd.common.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.GsonBuilder;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;

/**
 * A utility class for working with files.
 *
 * @author Keith Gamble
 */
public class FileUtilities {
	private static final Logger logger = LoggerFactory.getLogger(FileUtilities.class.getName());

	/**
	 * Sorts the given JSON element recursively. This method will sort the keys in
	 * JSON objects and the elements in JSON arrays. This method is useful for
	 * comparing JSON objects that may have the same elements but in a different
	 * order.
	 *
	 * @param element the JSON element to sort
	 * @return the sorted JSON element
	 */
	public static JsonElement sortJsonElementRecursively(JsonElement element) {
		if (element.isJsonObject()) {
			JsonObject object = element.getAsJsonObject();
			JsonObject sortedObject = new JsonObject();

			object.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.forEach(entry -> sortedObject.add(entry.getKey(), sortJsonElementRecursively(entry.getValue())));

			return sortedObject;
		} else if (element.isJsonArray()) {
			JsonArray array = element.getAsJsonArray();
			JsonArray sortedArray = new JsonArray();

			List<String> elementStrings = new ArrayList<>();
			for (JsonElement arrayElement : array) {
				elementStrings.add(arrayElement.toString());
			}
			Collections.sort(elementStrings);

			for (String elementString : elementStrings) {
				for (JsonElement arrayElement : array) {
					if (arrayElement.toString().equals(elementString)) {
						sortedArray.add(sortJsonElementRecursively(arrayElement));
						break;
					}
				}
			}

			return sortedArray;
		} else {
			return element;
		}
	}

	/**
	 * Saves the given JSON object to the given file path.
	 * 
	 * @param json     the JSON object to save
	 * @param filePath the file path to save the JSON object to
	 * @throws IOException if there is an error writing the JSON object to the file
	 */
	public static void saveJsonToFile(JsonObject json, String filePath) throws IOException {
		File file = new File(filePath);
		File parentDir = file.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			if (!parentDir.mkdirs()) {
				throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
			}
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		json = (JsonObject) sortJsonElementRecursively(json);
		String prettyJson = gson.toJson(json);

		try (FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(prettyJson);
		}
	}

	/**
	 * Reads the contents of the given file and returns it as a string.
	 * 
	 * @param file the file to read
	 * @return the contents of the file as a string
	 * @throws IOException if there is an error reading the file
	 */
	public static String readFileAsString(File file) throws IOException {
		logger.trace("Reading file as string: " + file.getAbsolutePath());
		StringBuilder stringBuilder = new StringBuilder();

		try (FileReader fileReader = new FileReader(file)) {
			int character;
			while ((character = fileReader.read()) != -1) {
				stringBuilder.append((char) character);
			}
		}

		return stringBuilder.toString();
	}

	/**
	 * Finds the _types_ folder in the given array of files.
	 *
	 * @param files the array of files to search
	 * @return the _types_ folder if it exists, otherwise null
	 */
	public static File findTypesFolder(File[] files) {
		for (File file : files) {
			if (file.isDirectory() && file.getName().equals("_types_")) {
				return file;
			}
		}
		return null;
	}

	/**
	 * Completely cleans a directory by removing all its contents.
	 * This ensures the export directory only contains current tag data.
	 *
	 * @param directoryPath the path to the directory to clean
	 * @throws IOException if there is an error during cleanup
	 */
	public static void cleanDirectory(String directoryPath) throws IOException {
		File directory = new File(directoryPath);
		if (directory.exists() && directory.isDirectory()) {
			logger.info("Cleaning directory: {}", directoryPath);
			deleteDirectoryContents(directory);
		}
	}

	/**
	 * Deletes existing files in the given directory that are not part of the
	 * individual files export structure defined by the JSON object. Tailored for
	 * the IndividualFilesExportStrategy.
	 *
	 * @param directoryPath the path to the directory to delete files from
	 * @param jsonToSave    the JSON object defining the tags to preserve
	 * @throws IOException if there is an error deleting files
	 */
	public static void deleteExistingFiles(String directoryPath, JsonObject jsonToSave) throws IOException {
		File directory = new File(directoryPath);
		if (!directory.exists() || !directory.isDirectory()) {
			return;
		}

		logger.info("Cleaning existing files in directory: {}", directoryPath);

		// Get list of expected files and directories based on JSON structure
		Set<String> expectedItems = collectExpectedItems(jsonToSave, "");

		File[] existingFiles = directory.listFiles();
		if (existingFiles != null) {
			for (File file : existingFiles) {
				String fileName = file.getName();

				if (file.isDirectory()) {
					if (!expectedItems.contains(fileName + "/")) {
						logger.debug("Removing unexpected directory: {}", file.getAbsolutePath());
						deleteDirectory(file);
					} else {
						// Directory is expected, recursively clean it
						JsonObject subFolder = findSubFolder(jsonToSave, fileName);
						if (subFolder != null) {
							deleteExistingFiles(file.getAbsolutePath(), subFolder);
						}
					}
				} else {
					String expectedJsonFile = fileName.endsWith(".json") ? fileName : fileName + ".json";
					if (!expectedItems.contains(expectedJsonFile)) {
						logger.debug("Removing unexpected file: {}", file.getAbsolutePath());
						if (!file.delete()) {
							logger.warn("Failed to delete file: {}", file.getAbsolutePath());
						}
					}
				}
			}
		}
	}

	/**
	 * Collects all expected file and directory names from the JSON structure.
	 *
	 * @param json        the JSON object to analyze
	 * @param currentPath the current path context (for logging)
	 * @return set of expected file and directory names
	 */
	private static Set<String> collectExpectedItems(JsonObject json, String currentPath) {
		Set<String> expectedItems = new HashSet<>();

		if (json.has("tags")) {
			JsonArray tags = json.getAsJsonArray("tags");
			for (JsonElement tag : tags) {
				if (tag.isJsonObject()) {
					JsonObject tagObject = tag.getAsJsonObject();
					String tagName = tagObject.get("name").getAsString();
					String tagType = tagObject.get("tagType").getAsString();

					if ("Folder".equals(tagType) || "Provider".equals(tagType)) {
						expectedItems.add(tagName + "/");
					} else {
						expectedItems.add(tagName + ".json");
					}
				}
			}
		}

		return expectedItems;
	}

	/**
	 * Finds a subfolder JSON object by name.
	 *
	 * @param json       the parent JSON object
	 * @param folderName the name of the folder to find
	 * @return the subfolder JSON object or null if not found
	 */
	private static JsonObject findSubFolder(JsonObject json, String folderName) {
		if (json.has("tags")) {
			JsonArray tags = json.getAsJsonArray("tags");
			for (JsonElement tag : tags) {
				if (tag.isJsonObject()) {
					JsonObject tagObject = tag.getAsJsonObject();
					String tagName = tagObject.get("name").getAsString();
					String tagType = tagObject.get("tagType").getAsString();

					if (("Folder".equals(tagType) || "Provider".equals(tagType)) &&
							tagName.equals(folderName)) {
						return tagObject;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Determines if the directory should be preserved based on the JSON array of
	 * tags.
	 * Assumes individual files export mode (one file per tag/udt/folder).
	 *
	 * @param tags          the JSON array of tags to check
	 * @param directoryName the name of the directory to check
	 * @return true if the directory should be preserved, otherwise false
	 */
	private static boolean shouldPreserveDirectory(JsonArray tags, String directoryName) {
		for (JsonElement tag : tags) {
			JsonObject tagObject = tag.getAsJsonObject();
			String tagType = tagObject.get("tagType").getAsString();
			if ("Folder".equals(tagType) || "Provider".equals(tagType)) {
				if (tagObject.get("name").getAsString().equals(directoryName)) {
					return true;
				}
			}
			if (tagObject.has("tags")) {
				if (shouldPreserveDirectory(tagObject.getAsJsonArray("tags"), directoryName)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determines if the file should be preserved based on the JSON array of tags.
	 * Assumes individual files export mode (one file per tag).
	 *
	 * @param tags     the JSON array of tags to check
	 * @param fileName the name of the file to check (without .json extension)
	 * @return true if the file should be preserved, otherwise false
	 */
	private static boolean shouldPreserveFile(JsonArray tags, String fileName) {
		String fileNameWithoutExtension = fileName.replace(".json", "");
		for (JsonElement tag : tags) {
			JsonObject tagObject = tag.getAsJsonObject();
			String tagType = tagObject.get("tagType").getAsString();
			if (!"Folder".equals(tagType) && !"Provider".equals(tagType)) {
				if (tagObject.get("name").getAsString().equals(fileNameWithoutExtension)) {
					return true;
				}
			}
			if (tagObject.has("tags")) {
				if (shouldPreserveFile(tagObject.getAsJsonArray("tags"), fileName)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Deletes all contents of a directory but preserves the directory itself.
	 *
	 * @param directory the directory whose contents should be deleted
	 * @throws IOException if there is an error during deletion
	 */
	public static void deleteDirectoryContents(File directory) throws IOException {
		if (!directory.exists() || !directory.isDirectory()) {
			return;
		}

		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDirectory(file);
				} else {
					if (!file.delete()) {
						throw new IOException("Failed to delete file: " + file.getAbsolutePath());
					}
				}
			}
		}
	}

	/**
	 * Deletes the given directory and all of its contents.
	 *
	 * @param directory the directory to delete
	 * @throws IOException if there is an error during deletion
	 */
	public static void deleteDirectory(File directory) throws IOException {
		if (!directory.exists()) {
			return;
		}

		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDirectory(file);
				} else {
					if (!file.delete()) {
						throw new IOException("Failed to delete file: " + file.getAbsolutePath());
					}
				}
			}
		}

		if (!directory.delete()) {
			throw new IOException("Failed to delete directory: " + directory.getAbsolutePath());
		}
	}

	/**
	 * Cleans a structured files export directory by removing all existing
	 * tags.json and udts.json files and empty directories.
	 *
	 * @param directoryPath the path to the directory to clean
	 * @throws IOException if there is an error during cleanup
	 */
	public static void cleanStructuredFilesDirectory(String directoryPath) throws IOException {
		File directory = new File(directoryPath);
		if (!directory.exists() || !directory.isDirectory()) {
			return;
		}

		logger.info("Cleaning structured files directory: {}", directoryPath);
		cleanStructuredFilesRecursively(directory);
	}

	/**
	 * Recursively cleans structured files directories.
	 *
	 * @param directory the directory to clean
	 * @throws IOException if there is an error during cleanup
	 */
	private static void cleanStructuredFilesRecursively(File directory) throws IOException {
		File[] files = directory.listFiles();
		if (files == null) {
			return;
		}

		for (File file : files) {
			if (file.isDirectory()) {
				cleanStructuredFilesRecursively(file);
				// Check if directory is empty after cleaning and remove it
				if (isDirectoryEmpty(file)) {
					logger.debug("Removing empty directory: {}", file.getAbsolutePath());
					if (!file.delete()) {
						logger.warn("Failed to delete empty directory: {}", file.getAbsolutePath());
					}
				}
			} else if (file.getName().equals("tags.json") || file.getName().equals("udts.json")) {
				logger.debug("Removing existing structured file: {}", file.getAbsolutePath());
				if (!file.delete()) {
					logger.warn("Failed to delete file: {}", file.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * Checks if a directory is empty.
	 *
	 * @param directory the directory to check
	 * @return true if the directory is empty, false otherwise
	 */
	private static boolean isDirectoryEmpty(File directory) {
		if (!directory.isDirectory()) {
			return false;
		}

		File[] files = directory.listFiles();
		return files == null || files.length == 0;
	}
}