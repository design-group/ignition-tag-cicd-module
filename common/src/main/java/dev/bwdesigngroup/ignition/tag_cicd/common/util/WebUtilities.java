/*
 * Copyright 2023 Barry-Wehmiller Design Group
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package dev.bwdesigngroup.ignition.tag_cicd.common.util;

import javax.servlet.http.HttpServletResponse;

import com.inductiveautomation.ignition.common.gson.JsonObject;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author Keith Gamble
 */
public class WebUtilities {

    /**
     * Returns a jsonObject to represent an HTTP error of status 400.
     * 
     * @param httpServletResponse the HttpServletResponse to set the status code on
     * @param message the message to include in the error response
     * @return a jsonObject to represent an HTTP error of status 400
     */
    public static JsonObject getBadRequestError(HttpServletResponse httpServletResponse, String message) {
        httpServletResponse.setStatus(400);
        JsonObject json = new JsonObject();
        json.addProperty("error", message);
        return json;
    }

    /**
     * Returns a jsonObject to represent an HTTP error of status 500.
     * 
     * @param httpServletResponse the HttpServletResponse to set the status code on
     * @param message the message to include in the error response
     * @return a jsonObject to represent an HTTP error of status 500
     */
    public static JsonObject getInternalServerErrorResponse(HttpServletResponse httpServletResponse, Exception e) {
        httpServletResponse.setStatus(500);
        JsonObject json = new JsonObject();
        json.addProperty("error", e.getMessage());
        json.addProperty("errorType", e.getClass().getName());

        // Get the stack trace as a string
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();
        json.addProperty("stackTrace", stackTrace);

        // Optional: You can also include additional information like line number and module
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        if (stackTraceElements.length > 0) {
            StackTraceElement topElement = stackTraceElements[0];
            json.addProperty("lineNumber", topElement.getLineNumber());
            json.addProperty("module", topElement.getClassName());
        }

        return json;
    }
}