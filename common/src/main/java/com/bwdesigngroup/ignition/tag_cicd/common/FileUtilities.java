/*
 * Copyright 2023 Barry-Wehmiller Design Group
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.bwdesigngroup.ignition.tag_cicd.common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.inductiveautomation.ignition.common.gson.JsonObject;

/**
 *
 * @author Keith Gamble
 */
public class FileUtilities {

	/**
	 * Saves the given JSON object to the given file path.
	 * @param json the JSON object to save
	 * @param filePath the file path to save the JSON object to
	 * @throws IOException if there is an error writing the JSON object to the file
	 */
	public static void saveJsonToFile(JsonObject json, String filePath) throws IOException {
		// Create the file if it doesn't exist
		File file = new File(filePath);
		file.createNewFile();

		// Write the JSON to the file
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(json.toString());
		fileWriter.close();
	}
}
