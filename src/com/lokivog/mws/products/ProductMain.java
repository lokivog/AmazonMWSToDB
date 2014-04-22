package com.lokivog.mws.products;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simpleorm.sessionjdbc.SSessionJdbc;
import simpleorm.utils.SLog;

import com.lokivog.mws.Constants;
import com.lokivog.mws.MWSUtils;
import com.lokivog.mws.config.StandaloneConfiguration;
import com.lokivog.mws.dao.AmazonProductDAO;

/**
 * The Class ProductMain is the main class for running AmazonMWSToDB.
 */
public class ProductMain {

	final static Logger logger = LoggerFactory.getLogger(ProductMain.class);
	public static Logger JSON_LOGGER = LoggerFactory.getLogger("jsonproductlogger");
	public static final Logger XML_LOGGER = LoggerFactory.getLogger("xmlproductlogger");
	public static final String DEFAULT_LOCAL_JSON_FILE = "output/json/amzproducts.json";
	public static final String DEFAULT_ID_TXT_FILE = "input/ids.txt";

	public static enum PROCESS_TYPE {
		IDS("ids"), JSON("json"), TABLES("tables"), SELLER_PRODUCTS("seller");
		private String mLoadType;

		private PROCESS_TYPE(String pLoadType) {
			mLoadType = pLoadType;
		}

		public String getValue() {
			return mLoadType;
		}
	}

	public static enum ID_LOAD_TYPE {
		INLINE_IDS("inline"), DATABASE("db"), JSON_FILE("json"), TXT_FILE("txt");

		private String mIdType;

		private ID_LOAD_TYPE(String pIdType) {
			mIdType = pIdType;
		}

		public String getValue() {
			return mIdType;
		}

		public static ID_LOAD_TYPE getIdLoadType(String pValue) {
			if (pValue.equals(INLINE_IDS.getValue())) {
				return INLINE_IDS;
			} else if (pValue.equals(DATABASE.getValue())) {
				return DATABASE;
			} else if (pValue.equals(JSON_FILE.getValue())) {
				return JSON_FILE;
			} else if (pValue.equals(TXT_FILE.getValue())) {
				return TXT_FILE;
			}
			return null;
		}
	}

	public static enum JSON_LOAD_TYPE {
		LOCAL("local"), DROPBOX("dropbox");

		private String mJSONType;

		private JSON_LOAD_TYPE(String pJSONType) {
			mJSONType = pJSONType;
		}

		public String getValue() {
			return mJSONType;
		}

		public static JSON_LOAD_TYPE getIdLoadType(String pValue) {
			if (pValue.equals(LOCAL.getValue())) {
				return LOCAL;
			} else if (pValue.equals(DROPBOX.getValue())) {
				return DROPBOX;
			}
			return null;
		}
	}

	public static void main(String[] args) {
		ProductMain productMain = new ProductMain();
		SLog.getSessionlessLogger().setLevel(0);

		// TODO get product category from request

		// String defaultProcessType = PROCESS_TYPE.TABLES.getValue();
		String defaultProcessType = PROCESS_TYPE.IDS.getValue();
		// String defaultProcessType = PROCESS_TYPE.SELLER_PRODUCTS.getValue();
		// String defaultProcessType = PROCESS_TYPE.JSON.getValue();
		String defaultIdLoadType = ID_LOAD_TYPE.DATABASE.getValue();
		// String defaultIdLoadType = ID_LOAD_TYPE.INLINE_IDS.getValue();
		// String defaultIdLoadType = ID_LOAD_TYPE.TXT_FILE.getValue();
		String defaultIdType = "UPC"; // values are UPC or ASIN
		// String defaultIdType = "ASIN";
		String defaultDropShipSource = Constants.DROP_SHIP_SOURCE_kOLE;
		String defaultIdTxtFile = DEFAULT_ID_TXT_FILE;
		// String defaultLocalJSONFile = DEFAULT_LOCAL_JSON_FILE;
		String defaultLocalJSONFile = "output/json";
		String defaultJSONLoadType = JSON_LOAD_TYPE.LOCAL.getValue();

		boolean update = false;
		boolean ignoreRecentlyProcessed = true;

		// TODO allow override args to be passed in via command line args
		String processType = defaultProcessType;

		if (processType.equals(PROCESS_TYPE.IDS.getValue())) {
			ID_LOAD_TYPE loadType = ID_LOAD_TYPE.getIdLoadType(defaultIdLoadType);
			logger.info("idLoadType: " + loadType);
			productMain.processIds(loadType, defaultIdType, defaultDropShipSource, defaultIdTxtFile, update,
					ignoreRecentlyProcessed, defaultLocalJSONFile);
		} else if (processType.equals(PROCESS_TYPE.JSON.getValue())) {
			JSON_LOAD_TYPE loadType = JSON_LOAD_TYPE.getIdLoadType(defaultJSONLoadType);
			productMain.processJSON(loadType, defaultLocalJSONFile, defaultDropShipSource, update);
		} else if (processType.equals(PROCESS_TYPE.TABLES.getValue())) {
			productMain.dropAndCreateTables();
		} else if (processType.equals(PROCESS_TYPE.SELLER_PRODUCTS.getValue())) {
			productMain.installSellerProducts(defaultIdTxtFile, defaultDropShipSource);
		}

	}

	public void dropAndCreateTables() {
		ProductManager pm = null;
		try {
			pm = new ProductManager(ProductScheduler.class.getSimpleName(), Constants.DROP_SHIP_SOURCE_kOLE);
			if (pm.initDBConnection()) {
				pm.dropTables(ProductManager.TABLES);
				pm.createTables(ProductManager.TABLES);
				// pm.createTables(SellerProductDAO.SELLER_PRODUCT);
			}
		} finally {
			if (pm != null) {
				pm.shutdownDB();
			}
		}
	}

	/**
	 * ProcessJSON inserts products into the amazon products table based on files containing json from previously queried amazon products.
	 * 
	 * @param pLoadType the load type
	 * @param pFileName the file name
	 * @param pDropShipSource the drop ship source
	 * @param pUpdate the update
	 */
	public void processJSON(JSON_LOAD_TYPE pLoadType, String pFileName, String pDropShipSource, boolean pUpdate) {
		switch (pLoadType) {
			case LOCAL: {
				processProductsFromLocalJSON(pFileName, pDropShipSource, pUpdate);
				break;
			}
			case DROPBOX: {
				processProductsFromDropbox(pDropShipSource, pUpdate);
				break;
			}
			default: {
				logger.info("processJSON: no JSON_LOAD_TYPE was selected. Program will exit");
				break;
			}
		}
	}

	/**
	 * Process ids.
	 * 
	 * @param pLoadType the load type
	 * @param pIdType the id type
	 * @param pDropShipSource the drop ship source
	 * @param pUpdate the update
	 * @param pIgnoreRecentlyProcessed the ignore recently processed
	 * @param pLocalJSONFile the local json file
	 */
	public void processIds(ID_LOAD_TYPE pLoadType, String pIdType, String pDropShipSource, String pTxtFileName,
			boolean pUpdate, boolean pIgnoreRecentlyProcessed, String pLocalJSONFile) {
		logger.info(
				"processIds: ID_LOAD_TYPE: {}, IdType: {}, DropShipSource: {} TxtFileName: {}, pUpdate: {}, IgnoreRecentlyProcessed: {}, LocalJSONFile: {}",
				pLoadType.getValue(), pIdType, pDropShipSource, pTxtFileName, pUpdate, pIgnoreRecentlyProcessed,
				pLocalJSONFile);

		ProductManager pm = null;
		try {
			pm = new ProductManager(ProductScheduler.class.getSimpleName(), pDropShipSource);
			if (pm.initDBConnection()) {
				List<String> ids = null;
				switch (pLoadType) {
					case INLINE_IDS: {
						ids = loadIdsInline();
						break;
					}
					case DATABASE: {
						ProductQueryManager pqm = new ProductQueryManager(pm);
						// ids = pqm.queryProductIdsToUpdate(new Date(), pIdType);
						// ids = pqm.querySellerProducts();
						ids = pqm.queryKoleProducts();
						logger.info("Ids : " + ids);
						break;
					}
					case TXT_FILE: {
						ids = loadIdsFromTxtFile(pTxtFileName);
						break;
					}
					default: {
						logger.info("processIds: no ID_LOAD_TYPE was selected. Program will exit");
						break;
					}
				}
				int batchNumber = 3;
				pm.setBatchNumber(batchNumber);
				if (ids != null && !ids.isEmpty()) {
					if (!pIgnoreRecentlyProcessed) {
						int sizeBefore = ids.size();
						Set<String> products = loadProductIdsFromJSON(pLocalJSONFile);
						RecentlyDownloadedPredicate recentlyDownloadedPredicate = new RecentlyDownloadedPredicate(
								products);
						CollectionUtils.filter(ids, recentlyDownloadedPredicate);
						logger.info(
								"filtered out recently processed ids from productId List size: {}, Before size: {}, after size: {}",
								products.size(), sizeBefore, ids.size());
					}
					if (!ids.isEmpty()) {
						List<List<String>> subLists = MWSUtils.split(ids, Constants.MAX_PRODUCT_LOOKUP_SIZE);
						int subListSize = subLists.size();
						int count = 1;
						logger.info("Total List size: {}, total items: ", subListSize, subListSize
								* Constants.MAX_PRODUCT_LOOKUP_SIZE);
						for (List<String> subList : subLists) {
							logger.info("Processing list {} of {}, items remaining: {}", count, subListSize,
									(subListSize - count) * Constants.MAX_PRODUCT_LOOKUP_SIZE);
							pm.findAndInsertProducts(subList, pUpdate, pIdType);
							count++;
						}
					} else {
						logger.info("processIds: no ids to process");
					}
				}

			}
		} finally {
			if (pm != null) {
				pm.shutdownDB();
			}
		}
	}

	/**
	 * Process json calls the ProductManager to insert json objects into the amz_product table.
	 * 
	 * @param jsonArray the json array
	 * @param pDropShipSource the drop ship source
	 * @param pUpdate the update
	 */
	private void processJSON(JSONArray jsonArray, String pDropShipSource, boolean pUpdate) {
		ProductManager pm = null;
		try {
			pm = new ProductManager(ProductMain.class.getSimpleName(), pDropShipSource);
			if (pm.initDBConnection()) {
				logger.info("Total JSONArray size: {}", jsonArray.length());
				pm.insertJSONProducts(jsonArray, pUpdate);
			}
		} finally {
			if (pm != null) {
				pm.shutdownDB();
			}
		}
	}

	/**
	 * Process products from dropbox loads previously process products stored in a dropbox account. The products should be saved out into
	 * json format.
	 * 
	 * @param pDropShipSource the drop ship source
	 * @param pUpdate the update
	 */
	private void processProductsFromDropbox(String pDropShipSource, boolean pUpdate) {
		logger.info(
				"processProductsFromDropbox: loading json products from dropbox: {}, dropShipSource: {}, Update: {}",
				pDropShipSource, pUpdate);
		installProductsFromDropBoxScript();
		String productsDir = StandaloneConfiguration.getInstance().getBuildProductsDir();
		File folder = new File(productsDir);
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry);
			} else {
				JSONArray array = loadAmazonProductsFromJSON(fileEntry);
				processJSON(array, pDropShipSource, pUpdate);
			}
		}
	}

	/**
	 * Process products from a local json file.
	 * 
	 * @param pFileName the file name
	 * @param pDropShipSource the drop ship source
	 * @param pUpdate the update
	 */
	private void processProductsFromLocalJSON(String pFileName, String pDropShipSource, boolean pUpdate) {
		logger.info("processProductsFromLocalJSON: loading json file from: {}, dropShipSource: {}, Update: {}",
				pFileName, pDropShipSource, pUpdate);
		JSONArray array = loadAmazonProductsFromJSON(new File(pFileName));
		processJSON(array, pDropShipSource, pUpdate);
	}

	private List<String> loadIdsInline() {
		List<String> ids = new ArrayList<String>();
		// ids.add("73101511064323433"); // invalid id
		// ids.add("731015155644"); // UPC from amazon that changed quantities
		// ids.add("B00ENHR1SU");
		ids.add("766539830081");
		return ids;
	}

	private List<String> loadIdsFromTxtFile(String pFileName) {
		List<String> ids = null;
		try {
			ids = FileUtils.readLines(new File(pFileName));
		} catch (IOException e) {
			logger.error("Error loading file: " + pFileName, e);
		}
		return ids;
	}

	public void installSellerProducts(String pFileName, String pDropShipSource) {
		List<String> productInfo = null;
		try {
			productInfo = FileUtils.readLines(new File(pFileName));
		} catch (IOException e) {
			logger.error("Error loading file: " + pFileName, e);
		}
		ProductManager pm = null;

		try {
			// ses.begin();
			pm = new ProductManager(ProductScheduler.class.getSimpleName(), pDropShipSource);

			if (pm.initDBConnection()) {
				SSessionJdbc ses = pm.getSession();
				ses.begin();
				ProductQueryManager pqm = new ProductQueryManager(pm);
				List<AmazonProductDAO> products = new ArrayList<AmazonProductDAO>(productInfo.size());
				for (String line : productInfo) {
					String[] info = line.split("\t");
					AmazonProductDAO product = pqm.queryAmazonProduct(info[0], info[1]);
					if (product != null) {
						products.add(product);
					}
				}
				// pm.insertSellerProduct(products);
				ses.commit();
			}

		} finally {
			pm.shutdownDB();
		}

	}

	private void installProductsFromDropBoxScript() {
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
		JSONArray jsonArray = new JSONArray();
		String currentJSONString = "";
		int count = 1;
		try {
			logger.info("loadAmazonProductsFromJSON from file: {}", pFile.getName());
			List<String> lines = FileUtils.readLines(pFile);
			if (lines != null && !lines.isEmpty()) {
				for (String line : lines) {
					currentJSONString = line;
					JSONObject currentObject = new JSONObject(line);
					jsonArray.put(currentObject);
					count++;
				}
			}

		} catch (FileNotFoundException e) {
			logger.error(Constants.OUTPUT_JSON_FILE + "not found", e);
		} catch (JSONException e) {
			logger.error("JSON parse exception line: " + count + ", string: " + currentJSONString, e);
		} catch (IOException e) {
			logger.error("IOException", e);
		}
		return jsonArray;
	}

	private Set<String> loadProductIdsFromJSON(String pFile) {
		Set<String> productIds = new HashSet<String>();
		int count = 1;
		try {
			logger.info("loadProductIdsFromJSON from file: {}", pFile);
			File file = new File(pFile);
			if (file.isDirectory()) {
				for (final File fileEntry : file.listFiles()) {
					if (fileEntry.isDirectory()) {
						listFilesForFolder(fileEntry);
					} else {
						productIds.addAll(loadProductIdsFromJSON(fileEntry));
					}
				}
			} else {
				productIds.addAll(loadProductIdsFromJSON(file));
			}

		} catch (JSONException e) {
			logger.error("JSONException processing line: {}", count, e);
		}
		return productIds;
	}

	private Set<String> loadProductIdsFromJSON(File pFile) {
		Set<String> productIds = null;
		int count = 1;
		try {
			logger.info("loadProductIdsFromJSON from file: {}", pFile);
			List<String> lines = FileUtils.readLines(pFile);
			productIds = new HashSet<String>(lines.size());
			for (String line : lines) {
				JSONObject object = new JSONObject(line);
				if (object.has("id")) {
					productIds.add(object.getString("id"));
				}
				count++;
			}

		} catch (JSONException e) {
			logger.error("JSONException processing line: {}", count, e);
		} catch (IOException e) {
			logger.error("IOException", e);
		}
		return productIds;
	}

	private void listFilesForFolder(final File folder) {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry);
			} else {
				System.out.println(fileEntry.getName());
			}
		}
	}

}

class RecentlyDownloadedPredicate implements Predicate<String> {

	private Set<String> set;

	public RecentlyDownloadedPredicate(Set<String> pSet) {
		set = pSet;
	}

	@Override
	public boolean evaluate(String pStr) {
		return set.contains(pStr);
	}
}
