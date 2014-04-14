package com.lokivog.mws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MWSUtils {

	final static Logger logger = LoggerFactory.getLogger(MWSUtils.class);

	/**
	 * Splits a list into multiple sub lists.
	 * 
	 * @param list the list
	 * @param i the i
	 * @return the list
	 */
	public static List split(final List list, int i) {
		int size = list.size();
		int number = size / i;
		int remain = size % i;
		if (remain != 0) {
			number++;
		}
		List out = new ArrayList<List>(number);
		for (int j = 0; j < number; j++) {
			int start = j * i;
			int end = start + i;
			if (end > list.size()) {
				end = list.size();
			}
			out.add(list.subList(start, end));
		}
		return out;
	}

	/**
	 * Converts an xml file to json.
	 * 
	 * @param pFileName the file name
	 * @return the jSON object
	 */
	public static JSONObject convertXMLFileToJSON(String pFileName) {
		StringBuilder builder = new StringBuilder();
		JSONObject object = null;
		int count = 1;
		try {
			logger.info("convertXMLFileToJSON for fileName: {}", pFileName);
			List<String> lines = FileUtils.readLines(new File(pFileName));
			for (String line : lines) {
				builder.append(line);
				count++;
			}
			object = XML.toJSONObject(builder.toString());
		} catch (FileNotFoundException e) {
			logger.error(Constants.OUTPUT_JSON_FILE + "not found", e);
		} catch (JSONException e) {
			logger.error("JSON parse exception line: " + count, e);
		} catch (IOException e) {
			logger.error("IOException", e);
		}
		return object;
	}

}
