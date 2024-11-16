/*
 * Copyright 2023 Barry-Wehmiller Design Group
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.bwdesigngroup.ignition.tag_cicd.common;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

			// Sort the keys in the object
			object.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.forEach(entry -> sortedObject.add(entry.getKey(), sortJsonElementRecursively(entry.getValue())));

			return sortedObject;
		} else if (element.isJsonArray()) {
			JsonArray array = element.getAsJsonArray();
			JsonArray sortedArray = new JsonArray();

			// Sort the elements in the array based on their string representation
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
			// For non-object and non-array elements, return the element as-is
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
		// logger.trace("Saving JSON to file: " + filePath);
		// Create the file if it doesn't exist
		File file = new File(filePath);
		file.createNewFile();

		// Create a Gson instance with pretty printing
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// Createa deterministically sorted JSON object
		json = (JsonObject) sortJsonElementRecursively(json);
		// Convert the Json object to a pretty printed string
		String prettyJson = gson.toJson(json);
		// Write the JSON to the file
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(prettyJson);
		fileWriter.close();
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

		FileReader fileReader = new FileReader(file);
		int character;
		while ((character = fileReader.read()) != -1) {
			stringBuilder.append((char) character);
		}
		fileReader.close();

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
	 * Deletes existing files in the given directory that should not be preserved
	 * based on the given JSON object. This method will delete files that are not
	 * tagged to be preserved in the JSON object. If the JSON object has a "tags"
	 * array, this method will recursively delete files that are not tagged to be
	 * preserved. If the JSON object does not have a "tags" array, this method will
	 * delete all files in the directory that are not tagged to be preserved.
	 *
	 * @param directoryPath          the path to the directory to delete files from
	 * @param jsonToSave             the JSON object to use for determining which
	 *                               files to preserve
	 * @param individualFilesPerObject whether each object should have its own file
	 * @throws IOException if there is an error deleting files
	 */
	public static void deleteExistingFiles(String directoryPath, JsonObject jsonToSave,
			boolean individualFilesPerObject) throws IOException {
		File directory = new File(directoryPath);
		File[] existingFiles = directory.listFiles();
		if (existingFiles != null) {
			for (File file : existingFiles) {
				if (file.isDirectory()) {
					// If the file is a directory, check if it should be preserved
					if (!jsonToSave.has("tags") || !shouldPreserveDirectory(jsonToSave.getAsJsonArray("tags"),
							file.getName(), individualFilesPerObject)) {
						// If the directory should not be preserved, delete it recursively
						deleteDirectory(file);
					}
				} else {
					// If the file is not a directory, check if it should be preserved
					if (!jsonToSave.has("tags") || !shouldPreserveFile(jsonToSave.getAsJsonArray("tags"),
							file.getName(), individualFilesPerObject)) {
						// If the file should not be preserved, delete it
						file.delete();
					}
				}
			}
		}
	}

	/**
	 * Determines if the directory at the given path should be preserved based on the
	 * given JSON array of tags. This method will return true if the directory name
	 * matches a folder or provider tag in the JSON array. If the JSON array has a
	 * "tags" array, this method will recursively check if the directory should be
	 * preserved based on the nested tags. If the JSON array does not have a "tags"
	 * array, this method will return false.
	 *
	 * @param tags                  the JSON array of tags to check
	 * @param directoryPath          the path to the directory to check
	 * @param individualFilesPerObject whether each object should have its own file
	 * @return true if the directory should be preserved, otherwise false
	 */
	private static boolean shouldPreserveDirectory(JsonArray tags, String directoryPath, boolean individualFilesPerObject) {
		String directoryName = new File(directoryPath).getName();
	
		for (JsonElement tag : tags) {
			JsonObject tagObject = tag.getAsJsonObject();
			if (tagObject.get("tagType").getAsString().equals("Folder") || tagObject.get("tagType").getAsString().equals("Provider")) {
				if (tagObject.get("name").getAsString().equals(directoryName)) {
					if (individualFilesPerObject && tagObject.has("tags")) {
						return shouldPreserveDirectory(tagObject.getAsJsonArray("tags"), directoryPath, individualFilesPerObject);
					} else {
						return true;
					}
				}
			}
			if (individualFilesPerObject && tagObject.has("tags")) {
				String nestedDirectoryPath = directoryPath + "/" + tagObject.get("name").getAsString();
				if (shouldPreserveDirectory(tagObject.getAsJsonArray("tags"), nestedDirectoryPath, individualFilesPerObject)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determines if the file at the given path should be preserved based on the
	 * given JSON array of tags. This method will return true if the file name
	 * matches a tag in the JSON array. If the JSON array has a "tags" array, this
	 * method will recursively check if the file should be preserved based on the
	 * nested tags. If the JSON array does not have a "tags" array, this method will
	 * return false.
	 *
	 * @param tags                  the JSON array of tags to check
	 * @param filePath              the path to the file to check
	 * @param individualFilesPerObject whether each object should have its own file
	 * @return true if the file should be preserved, otherwise false
	 */
	private static boolean shouldPreserveFile(JsonArray tags, String filePath, boolean individualFilesPerObject) {
		String fileName = new File(filePath).getName().replace(".json", "");

		for (JsonElement tag : tags) {
			JsonObject tagObject = tag.getAsJsonObject();
			if (!tagObject.get("tagType").getAsString().equals("Folder")
					&& !tagObject.get("tagType").getAsString().equals("Provider")) {
				if (tagObject.get("name").getAsString().equals(fileName)) {
					return true;
				}
			}
			if (individualFilesPerObject && tagObject.has("tags")) {
				if (shouldPreserveFile(tagObject.getAsJsonArray("tags"), filePath, individualFilesPerObject)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Deletes the given directory and all of its contents.
	 *
	 * @param directory the directory to delete
	 */
	private static void deleteDirectory(File directory) {
		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDirectory(file);
				} else {
					file.delete();
				}
			}
		}
		directory.delete();
	}
}