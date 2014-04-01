package com.lokivog.mws.products;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simpleorm.dataset.SQuery;
import simpleorm.dataset.SQueryResult;
import simpleorm.sessionjdbc.SSessionJdbc;
import simpleorm.utils.SLog;

public class ProductManager {

	public static final String DRIVER = "org.hsqldb.jdbcDriver";
	public static final String SESSSION_NAME = "Tiny";
	final Logger logger = LoggerFactory.getLogger(ProductManager.class);

	private Connection connection;

	private boolean mPurgeProducts = false;

	public ProductManager() {
		try {
			Class.forName(DRIVER);
			connection = java.sql.DriverManager.getConnection("jdbc:hsqldb:hsqlTempFiles;shutdown=true;", "sa", "");
		} catch (SQLException e) {
			logger.error("Error getting connection", e);
		} catch (ClassNotFoundException e) {
			logger.error("Driver class not found for: " + DRIVER, e);
		}
	}

	public void shutdownDB() {
		SSessionJdbc ses = getSession();
		ses.close();
	}

	public void createProductTable() {
		SSessionJdbc ses = getSession();
		ses.begin();
		ses.rawUpdateDB(ses.getDriver().createTableSQL(Product.PRODUCT));
		ses.commit();
	}

	public void dropProductTable() {
		SSessionJdbc ses = getSession();
		ses.begin();
		ses.getDriver().dropTableNoError("XX_PRODUCT");
		ses.commit();
	}

	private SSessionJdbc getSession() {
		SSessionJdbc ses = SSessionJdbc.getThreadLocalSession();
		if (ses == null) {
			ses = SSessionJdbc.open(connection, SESSSION_NAME);
		}
		return ses;
	}

	public void findAndInsertProducts(List<DropShipProduct> pDropShipProducts) {
		List<String> productIds = filterExistingProducts(pDropShipProducts);
		logger.info("product ids: " + productIds);
		if (!productIds.isEmpty()) {
			SSessionJdbc ses = getSession();
			ses.begin();
			GetMatchingProductForId matchingProducts = new GetMatchingProductForId(productIds);
			JSONArray json = matchingProducts.matchProducts();
			bulkInsert(json, ses);
			ses.commit();
		} else {
			logger.info("No products to process from list {}", pDropShipProducts.toString());
		}
	}

	private List<String> filterExistingProducts(List<DropShipProduct> pDropShipProducts) {
		List<String> productIds = new ArrayList<String>();
		SSessionJdbc ses = getSession();
		ses.begin();
		ListIterator<DropShipProduct> iter = pDropShipProducts.listIterator();
		while (iter.hasNext()) {
			DropShipProduct dp = iter.next();
			SQuery<Product> productQuery = new SQuery<Product>(Product.PRODUCT).eq(Product.UPC, dp.getUPC());
			List<Product> products = ses.query(productQuery);
			logger.info("products returned from query: " + products);
			if (products.isEmpty()) {
				productIds.add(dp.getUPC());
			} else {
				logger.info("product already exist for id {}", dp.getUPC());
			}
		}
		ses.commit();
		return productIds;
	}

	public void printQueryResults() {
		SSessionJdbc ses = getSession();
		ses.begin();
		SQueryResult<Product> res = ses.query(new SQuery(Product.PRODUCT));
		SLog.getSessionlessLogger().message("Departments: " + res);
		for (Product product : res) {
			logger.info("----------- New Product ------------");
			Set<String> fieldNames = product.getMeta().getFieldNames();
			for (String fieldName : fieldNames) {
				logger.info(fieldName + ": " + product.getObject(product.getMeta().getField(fieldName)));
			}
		}
		ses.commit();
	}

	private List split(List list, int i) {
		List<List<String>> out = new ArrayList<List<String>>();
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

	private void bulkInsert(JSONArray pJSONArray, SSessionJdbc ses) {
		logger.info("pJSONArray: " + pJSONArray);

		int arraySize = pJSONArray.length();

		for (int a = 0; a < arraySize; a++) {
			JSONObject object = pJSONArray.getJSONObject(a);

			JSONArray products = object.getJSONArray("products");
			int size = products.length();
			for (int i = 0; i < size; i++) {
				Product product = ses.createWithGeneratedKey(Product.PRODUCT);
				product.setString(Product.UPC, object.getString("id"));
				product.setString(Product.STATUS, object.getString("status"));
				JSONObject obj = products.getJSONObject(i);
				JSONArray prodNames = obj.names();
				int prodSize = prodNames.length();
				for (int j = 0; j < prodSize; j++) {
					String name = prodNames.getString(j);
					// logger.info("setting: " + name);
					product.setObject(product.getMeta().getField(name.toUpperCase()), obj.getString(name));
					// logger.info(name + ":" + obj.getString(name));
					// allNames.add(name.toLowerCase() + " " + "VARCHAR(40)");
				}
			}
		}
	}

	public boolean isPurgeProducts() {
		return mPurgeProducts;
	}

	public void setPurgeProducts(boolean pPurgeProducts) {
		mPurgeProducts = pPurgeProducts;
	}

}
