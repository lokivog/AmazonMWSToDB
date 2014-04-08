package com.lokivog.mws.products;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simpleorm.utils.SLog;

import com.lokivog.mws.Constants;
import com.lokivog.mws.MWSUtils;
import com.lokivog.mws.config.StandaloneConfiguration;

public class ProductMain {

	final Logger logger = LoggerFactory.getLogger(ProductMain.class);

	private final static String DEFAULT_PRODUCTS_FILE = "output/products.txt";

	public static void main(String[] args) {
		ProductMain productMain = new ProductMain();
		// productMain.installProductsFromDropBox();
		// productMain.runIds();
		// productMain.getJSONProduct();
		productMain.conertToJSON();
	}

	public void runIds() {
		ProductManager pm = null;
		try {
			pm = new ProductManager(ProductScheduler.class.getSimpleName(), Constants.DROP_SHIP_SOURCE_kOLE);
			if (pm.initDBConnection()) {
				SLog.getSessionlessLogger().setLevel(0);
				List<String> ids = new ArrayList<String>();
				// ids.add("73101511064323433");
				ids.add("017874006052");
				// ids = loadProductIds();
				boolean update = false;

				List<List<String>> subLists = MWSUtils.split(ids, Constants.MAX_PRODUCT_LOOKUP_SIZE);
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
			// ids.add("731015109036");

		} finally {
			if (pm != null) {
				pm.shutdownDB();
			}
		}
	}

	public void run(JSONArray jsonArray) {
		ProductManager pm = null;
		try {
			pm = new ProductManager(ProductMain.class.getSimpleName(), Constants.DROP_SHIP_SOURCE_kOLE);
			if (pm.initDBConnection()) {
				SLog.getSessionlessLogger().setLevel(0);
				boolean update = false;
				logger.info("Total JSONArray size: {}", jsonArray.length());
				pm.insertJSONProducts(jsonArray, update);
			}

		} finally {
			if (pm != null) {
				pm.shutdownDB();
			}
		}
	}

	public void installProductsFromDropBox() {
		// runinstallProductsFromDropBoxScript();
		String productsDir = StandaloneConfiguration.getInstance().getBuildProductsDir();
		File folder = new File(productsDir);
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry);
			} else {
				JSONArray array = loadAmazonProductsFromJSON(fileEntry);
				run(array);
				// System.out.println(fileEntry.getName());
			}
		}
	}

	private void runinstallProductsFromDropBoxScript() {
		Runtime r = Runtime.getRuntime();
		BufferedReader br = null;
		try {
			Process p = r.exec("scripts/install_products_from_dropbox.sh");
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

	public void conertToJSON() {
		File file = new File("output/testxml.xml");
		FileReader reader = null;
		BufferedReader br = null;
		String currentJSONString = null;
		StringBuilder builder = new StringBuilder();
		int count = 0;
		try {
			logger.info("Processing file: {}", file.getName());
			reader = new FileReader(file);
			br = new BufferedReader(reader);

			while ((currentJSONString = br.readLine()) != null) {
				// create new JSONObject
				// System.out.println("processing line: " + count++);
				builder.append(currentJSONString);
				count++;
			}
		} catch (FileNotFoundException e) {
			logger.error(Constants.OUTPUT_JSON_FILE + "not found", e);
		} catch (JSONException e) {
			logger.error("JSON parse exception line: " + count + ", string: " + currentJSONString, e);
		} catch (IOException e) {
			logger.error("IOException", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					logger.error("IOException", e);
				}
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("IOException", e);
				}
			}
		}
		JSONObject object = XML.toJSONObject(builder.toString());
		logger.info("object: {}", object);
	}

	private void getJSONProduct() {
		Runtime r = Runtime.getRuntime();
		BufferedReader br = null;
		try {
			System.out.println("executing command: ");
			String[] commands = { "curl", "-H", "Accept: application/vnd.koleimports.ds.product+json",
					"https://X19385:536575080835a979c6b82468681325d415bfc49e@api.koleimports.com/products/BE003" };
			Process p = r.exec(commands);
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

	private JSONArray loadAmazonProductsFromJSON(File pFile) {
		FileReader reader = null;
		BufferedReader br = null;
		JSONArray jsonArray = new JSONArray();
		String currentJSONString = "";
		int count = 0;
		try {
			logger.info("Processing file: {}", pFile.getName());
			reader = new FileReader(pFile);
			br = new BufferedReader(reader);

			while ((currentJSONString = br.readLine()) != null) {
				// create new JSONObject
				// System.out.println("processing line: " + count++);
				JSONObject currentObject = new JSONObject(currentJSONString);
				jsonArray.put(currentObject);
			}
		} catch (FileNotFoundException e) {
			logger.error(Constants.OUTPUT_JSON_FILE + "not found", e);
		} catch (JSONException e) {
			logger.error("JSON parse exception line: " + count + ", string: " + currentJSONString, e);
		} catch (IOException e) {
			logger.error("IOException", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					logger.error("IOException", e);
				}
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("IOException", e);
				}
			}
		}
		return jsonArray;
	}

	private List<String> loadProductIds(String pFile) {
		List<String> productIds = new ArrayList<String>(100);
		FileReader reader = null;
		BufferedReader br = null;
		try {
			reader = new FileReader(pFile);
			br = new BufferedReader(reader);
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
					logger.error("IOException", e);
				}
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("IOException", e);
				}
			}
		}
		return productIds;
	}

	public void listFilesForFolder(final File folder) {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry);
			} else {
				System.out.println(fileEntry.getName());
			}
		}
	}

}
