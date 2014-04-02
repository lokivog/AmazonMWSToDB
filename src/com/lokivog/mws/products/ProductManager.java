package com.lokivog.mws.products;

import static com.lokivog.mws.Constants.ASIN;
import static com.lokivog.mws.Constants.DROP_SHIP_SOURCE_DEFAULT;
import static com.lokivog.mws.Constants.FEATURE;
import static com.lokivog.mws.Constants.FEATURE_LENGTH;
import static com.lokivog.mws.Constants.ID_TYPE;
import static com.lokivog.mws.Constants.MARKET_PLACE_ID;
import static com.lokivog.mws.Constants.STATUS;
import static com.lokivog.mws.Constants.STATUS_SUCCESS;
import static com.lokivog.mws.Constants.UPC;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lokivog.mws.Constants;
import com.lokivog.mws.dao.AmazonProductDAO;
import com.lokivog.mws.dao.AmazonProductErrorDAO;

import simpleorm.dataset.SQuery;
import simpleorm.dataset.SQueryResult;
import simpleorm.sessionjdbc.SSessionJdbc;
import simpleorm.utils.SLog;

public class ProductManager {

	public static final String DRIVER = "org.hsqldb.jdbcDriver";
	public static final String POSTGRES_DRIVER = "org.postgresql.Driver";
	final Logger logger = LoggerFactory.getLogger(ProductManager.class);
	private Connection connection;
	private String mSessionName;
	private String mDropShipSource;

	public ProductManager(String pSessionName) {
		try {
			setSessionName(pSessionName);
			setDropShipSource(DROP_SHIP_SOURCE_DEFAULT);
			Class.forName(DRIVER);
			connection = java.sql.DriverManager.getConnection("jdbc:hsqldb:hsqlTempFiles;shutdown=true;", "sa", "");
		} catch (SQLException e) {
			logger.error("Error getting connection", e);
		} catch (ClassNotFoundException e) {
			logger.error("Driver class not found for: " + DRIVER, e);
		}
	}

	public ProductManager(String pSessionName, String pDropShipSource) {
		try {
			setSessionName(pSessionName);
			setDropShipSource(pDropShipSource);
			Class.forName(POSTGRES_DRIVER);
			// connection = java.sql.DriverManager.getConnection("jdbc:hsqldb:hsqlTempFiles;shutdown=true;", "sa", "");
			String url = "jdbc:postgresql://localhost/amazon";
			connection = java.sql.DriverManager.getConnection(url, "postgres", "postgres");
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

	public void createTables() {
		SSessionJdbc ses = getSession();
		ses.begin();
		ses.rawUpdateDB(ses.getDriver().createTableSQL(AmazonProductDAO.PRODUCT));
		ses.rawUpdateDB(ses.getDriver().createTableSQL(AmazonProductErrorDAO.PRODUCT_ERROR));
		ses.commit();
	}

	public void dropTables() {
		SSessionJdbc ses = getSession();
		ses.begin();
		ses.getDriver().dropTableNoError(Constants.TABLE_PRODUCT);
		ses.getDriver().dropTableNoError(Constants.TABLE_PRODUCT_ERROR);
		ses.commit();
	}

	private SSessionJdbc getSession() {
		SSessionJdbc ses = SSessionJdbc.getThreadLocalSession();
		if (ses == null) {
			ses = SSessionJdbc.open(connection, getSessionName());
		}
		return ses;
	}

	public void findAndInsertProducts(List<String> pProductUPCList, boolean pUpdate) {
		List<String> productIds = filterExistingProducts(pProductUPCList, pUpdate);
		logger.debug("product ids: " + productIds);
		if (!productIds.isEmpty()) {
			boolean success = false;
			SSessionJdbc ses = null;
			GetMatchingProductForId matchingProducts = new GetMatchingProductForId(productIds);
			JSONArray json = matchingProducts.matchProducts();
			try {
				ses = getSession();
				ses.begin();
				bulkInsert(json, pUpdate);
				success = true;
			} catch (Exception e) {
				logger.error("Error during bulkInsert", e);
			} finally {
				if (ses != null) {
					if (!success) {
						ses.rollback();
					} else {
						ses.commit();
					}
				}
			}

		} else {
			logger.info("No products to process from list {}", pProductUPCList.toString());
		}
	}

	private List<String> filterExistingProducts(List<String> pProductUPCList, boolean pUpdate) {
		List<String> productIds = new ArrayList<String>();
		SSessionJdbc ses = getSession();
		ses.begin();
		for (String upc : pProductUPCList) {
			if (!pUpdate) {
				SQuery<AmazonProductDAO> productQuery = new SQuery<AmazonProductDAO>(AmazonProductDAO.PRODUCT).eq(AmazonProductDAO.UPC, upc).eq(
						AmazonProductDAO.DROP_SHIP_SOURCE, getDropShipSource());
				List<AmazonProductDAO> products = ses.query(productQuery);
				logger.debug("products returned from query: " + products);
				if (products.isEmpty()) {
					productIds.add(upc);
				} else {
					logger.info("product already exist for upc: {}, source: {}", upc, getDropShipSource());
				}
			} else {
				productIds.add(upc);
			}
		}
		ses.commit();
		return productIds;
	}

	public void printQueryResults() {
		SSessionJdbc ses = getSession();
		ses.begin();
		SQueryResult<AmazonProductDAO> res = ses.query(new SQuery(AmazonProductDAO.PRODUCT));
		SLog.getSessionlessLogger().message("Departments: " + res);
		for (AmazonProductDAO product : res) {
			logger.info("----------- New Product ------------");
			Set<String> fieldNames = product.getMeta().getFieldNames();
			for (String fieldName : fieldNames) {
				logger.info(fieldName + ": " + product.getObject(product.getMeta().getField(fieldName)));
			}
		}
		ses.commit();
	}

	private void bulkInsert(JSONArray pJSONArray, boolean pUpdate) {
		logger.info("pJSONArray: " + pJSONArray);

		int arraySize = pJSONArray.length();

		for (int i = 0; i < arraySize; i++) {
			JSONObject upcProducts = pJSONArray.getJSONObject(i);
			boolean amazonProductError = insertProductError(upcProducts);
			if (!amazonProductError) {
				JSONArray products = upcProducts.getJSONArray("products");
				int size = products.length();
				for (int j = 0; j < size; j++) {
					JSONObject jsonProduct = products.getJSONObject(j);

					String marketPlaceId = jsonProduct.getString(MARKET_PLACE_ID);
					String asin = jsonProduct.getString(ASIN);
					SSessionJdbc ses = getSession();
					AmazonProductDAO productRow = ses.findOrCreate(AmazonProductDAO.PRODUCT, marketPlaceId, asin);
					if (productRow.isNewRow() || pUpdate) {
						if (!productRow.isNewRow() && pUpdate) {
							logger.debug("Updating product: {}", asin);
						}
						productRow.setString(AmazonProductDAO.UPC, upcProducts.getString(UPC));
						productRow.setString(AmazonProductDAO.STATUS, upcProducts.getString(STATUS));
						productRow.setString(AmazonProductDAO.ELASTICSEARCH_ID, marketPlaceId + ":" + asin);
						productRow.setString(AmazonProductDAO.DROP_SHIP_SOURCE, getDropShipSource());

						JSONArray prodNames = jsonProduct.names();
						int prodSize = prodNames.length();
						for (int k = 0; k < prodSize; k++) {

							String name = prodNames.getString(k);
							if (name.equalsIgnoreCase(ASIN) || name.equalsIgnoreCase(MARKET_PLACE_ID)) {
								// do not reset asin or marketplaceId, these represent primary key of a product
								continue;
							} else if (name.equalsIgnoreCase(FEATURE)) {
								String feature = jsonProduct.getString(name);
								if (feature.length() > FEATURE_LENGTH) {
									// trim feature to FEATURE_LENGTH to match DB column length
									feature = feature.substring(0, FEATURE_LENGTH - 1);
								}
								productRow.setObject(productRow.getMeta().getField(name.toUpperCase()), feature);
							} else {
								productRow.setObject(productRow.getMeta().getField(name.toUpperCase()),
										jsonProduct.getString(name));
							}
						}

					} else {
						logger.warn("Product should never exist at this point, must investigate {}", jsonProduct);
					}
				}
			}
		}
	}

	public boolean insertProductError(JSONObject upcProducts) {
		boolean isProductError = false;
		String amazonStatus = upcProducts.getString(STATUS);
		if (!amazonStatus.equalsIgnoreCase(STATUS_SUCCESS)) {
			isProductError = true;
			String amazonUPC = upcProducts.getString(UPC);
			String amazonIdType = upcProducts.getString(ID_TYPE);
			SSessionJdbc ses = getSession();
			AmazonProductErrorDAO productError = ses.createWithGeneratedKey(AmazonProductErrorDAO.PRODUCT_ERROR);
			productError.setString(AmazonProductErrorDAO.UPC, amazonUPC);
			productError.setString(AmazonProductErrorDAO.ID_TYPE, amazonIdType);
			productError.setString(AmazonProductErrorDAO.STATUS, amazonStatus);
			productError.setObject(AmazonProductErrorDAO.JSON, upcProducts);
		}
		return isProductError;
	}

	public String getDropShipSource() {
		return mDropShipSource;
	}

	public void setDropShipSource(String pDropShipSource) {
		mDropShipSource = pDropShipSource;
	}

	public String getSessionName() {
		return mSessionName;
	}

	public void setSessionName(String pSessionName) {
		mSessionName = pSessionName;
	}

}
