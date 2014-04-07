package com.lokivog.mws.products;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simpleorm.utils.SLog;

import com.lokivog.mws.Constants;

/**
 * The Class ProductScheduler is the main class to run for retrieving products from Amazon.
 */
public class ProductScheduler {

	final Logger logger = LoggerFactory.getLogger(ProductScheduler.class);

	// public static final Logger JSON_LOGGER = LoggerFactory.getLogger("jsonproductlogger");
	public final static Logger JSON_LOGGER = LoggerFactory.getLogger("jsonproductlogger");
	public static final Logger XML_LOGGER = LoggerFactory.getLogger("xmlproductlogger");

	public void testSplit() {
		List<String> ids = new ArrayList<String>();
		ids.add("731015110643");
		ids.add("731015110650");
		// ids.add("731015110704");
		// ids.add("731015110810");
		List<List> subLists = split(ids, 1);
		int count = 1;
		for (List subList : subLists) {
			logger.info("list: {}, size: {}", count++, subList.size());
		}
	}

	public void run() {
		ProductManager pm = null;
		try {
			pm = new ProductManager(ProductScheduler.class.getSimpleName(), Constants.DROP_SHIP_SOURCE_kOLE);
			if (pm.initDBConnection()) {
				SLog.getSessionlessLogger().setLevel(0);
				// SLog.setSlogClass(SLogSlf4j.class);

				// pm.dropTables();
				// pm.createTables();
				// pm.printQueryResults();
				List<String> ids = new ArrayList<String>();
				// ids.add("73101511064323433");
				ids.add("731015110650");
				ids = loadProductIds();
				boolean update = false;
				boolean insertProductsFromJson = false;

				if (!insertProductsFromJson) {
					List<List<String>> subLists = split(ids, Constants.MAX_PRODUCT_LOOKUP_SIZE);
					int subListSize = subLists.size();
					int count = 1;
					logger.info("Total List size: {}, total items: ", subListSize, subListSize
							* Constants.MAX_PRODUCT_LOOKUP_SIZE);
					for (List<String> subList : subLists) {
						logger.info("Processing list {} of {}, items remaining: {}", count, subListSize,
								(subListSize - count) * Constants.MAX_PRODUCT_LOOKUP_SIZE);
						pm.findAndInsertProducts(subList, update);
						count++;
					}
				}

				if (insertProductsFromJson) {
					JSONArray jsonArray = loadAmazonProductsFromJSON();
					logger.info("Total JSONArray size: {}", jsonArray.length());

					pm.insertJSONProducts(jsonArray, update);
				}
				// logger.info("JsonArray: {}", jsonArray);

			}
			// ids.add("731015109036");

		} finally {
			if (pm != null) {
				pm.shutdownDB();
			}
		}
	}

	private List<String> loadProductIds() {
		List<String> productIds = new ArrayList<String>(110);
		FileReader reader = null;
		try {
			reader = new FileReader("output/products1.txt");
			BufferedReader br = new BufferedReader(reader);
			String line = "";
			while ((line = br.readLine()) != null) {
				productIds.add(line);
			}
		} catch (FileNotFoundException e) {
			logger.error("File not found", e);
		} catch (IOException e) {
			logger.error("IOException", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return productIds;
	}

	private JSONArray loadAmazonProductsFromJSON() {
		FileReader reader = null;
		JSONArray jsonArray = new JSONArray();
		try {
			reader = new FileReader(Constants.OUTPUT_JSON_FILE);
			BufferedReader br = new BufferedReader(reader);
			String currentJSONString = "";
			while ((currentJSONString = br.readLine()) != null) {
				// create new JSONObject
				JSONObject currentObject = new JSONObject(currentJSONString);
				jsonArray.put(currentObject);

			}
		} catch (FileNotFoundException e) {
			logger.error(Constants.OUTPUT_JSON_FILE + "not found", e);
		} catch (JSONException e) {
			logger.error("JSON parse exception", e);
		} catch (IOException e) {
			logger.error("IOException", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return jsonArray;
	}

	public void syncDropBox() {
		Runtime r = Runtime.getRuntime();
		BufferedReader br = null;
		try {
			Process p = r.exec("scripts/sync_products_to_dropbox.sh");
			p.waitFor();
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";

			while ((line = br.readLine()) != null) {
				logger.info(line);
			}
		} catch (IOException e) {
			logger.error("IOException", e);
		} catch (InterruptedException e) {
			logger.error("InterruptedException", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("IOException", e);
				}
			}
		}
	}

	/**
	 * Splits a list into multiple sub lists.
	 * 
	 * @param list the list
	 * @param i the i
	 * @return the list
	 */
	private List split(List list, int i) {
		List out = new ArrayList<List>();
		int size = list.size();
		int number = size / i;
		int remain = size % i;
		if (remain != 0) {
			number++;
		}
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

	public void testlog() {
		logger.info("Amazon Response Error - Type: Sender, Code: InvalidParameterValue, Message: Invalid UPC identifier 70659812224 for marketplace ATVPDKIKX0DER");
	}

	public static void main(String[] args) {
		ProductScheduler scheduler = new ProductScheduler();
		scheduler.syncDropBox();
		scheduler.run();

		// ProcessFeed processFeed = new ProcessFeed();
		// processFeed.processFeed();
		// scheduler.testlog();

		// File file = new File("output/amazonproducts.json");
		// System.out.println(file.length() / 1024 / 1024);
		// scheduler.testSplit();
	}

	public List<String> getProductIds() {
		List<String> ids = new ArrayList<String>();
		ids.add("731015140640");
		ids.add("633040306001");
		ids.add("731015006403");
		ids.add("731015023950");
		ids.add("731015004560");
		// second 5
		ids.add("731015081684");
		ids.add("731015000012");
		ids.add("731015109357");
		ids.add("731015109111");
		ids.add("731015109289");
		// thrid 5
		ids.add("731015110643");
		ids.add("731015110650");
		ids.add("731015110704");
		ids.add("731015110810");
		ids.add("731015109036");
		ids.add("731015109302");
		ids.add("731015109524");
		ids.add("731015109777");
		ids.add("731015110001");
		ids.add("731015110032");
		ids.add("731015110056");
		ids.add("731015110063");
		ids.add("731015110070");
		ids.add("731015110582");
		ids.add("731015110476");
		ids.add("731015110797");
		ids.add("731015110803");
		ids.add("731015109456");
		ids.add("731015109463");
		return ids;
	}

}
