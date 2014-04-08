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

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simpleorm.dataset.SFieldMeta;
import simpleorm.dataset.SFieldScalar;
import simpleorm.dataset.SFieldString;
import simpleorm.dataset.SQuery;
import simpleorm.dataset.SQueryResult;
import simpleorm.dataset.SRecordMeta;
import simpleorm.sessionjdbc.SSessionJdbc;
import simpleorm.utils.SLog;

import com.lokivog.mws.config.StandaloneConfiguration;
import com.lokivog.mws.dao.AmazonProductDAO;
import com.lokivog.mws.dao.AmazonProductErrorDAO;

public class ProductManager {

	public static final String DRIVER = "org.hsqldb.jdbcDriver";
	public static final String POSTGRES_DRIVER = "org.postgresql.Driver";

	public static final SRecordMeta<?>[] TABLES = { AmazonProductDAO.PRODUCT, AmazonProductErrorDAO.PRODUCT_ERROR };
	final Logger logger = LoggerFactory.getLogger(ProductManager.class);

	// database properties
	private Connection connection = null;
	private String mDriverName;

	/** The Session name for the SimpleDAO connection session object. */
	private String mSessionName;

	/** The Drop ship source name to identify the dropship vendor the product originated from. */
	private String mDropShipSource;

	private Date mStartDate = new Date();

	private boolean mConnectionEstablished = false;

	/**
	 * Instantiates a new product manager with a name for the SimpleDAO session object. If used the dropShipSource is set to
	 * DROP_SHIP_SOURCE_DEFAULT by default.
	 * 
	 * @param pSessionName the session name
	 */
	public ProductManager(String pSessionName) {
		setSessionName(pSessionName);
		setDropShipSource(DROP_SHIP_SOURCE_DEFAULT);
		initDBConnection();
		// Class.forName(DRIVER);
		// connection = java.sql.DriverManager.getConnection("jdbc:hsqldb:hsqlTempFiles;shutdown=true;", "sa", "");

	}

	/**
	 * Instantiates a new product manager with a name for the SimpleDAO session object and a name for the drop ship source.
	 * 
	 * @param pSessionName the session name
	 * @param pDropShipSource the drop ship source
	 */
	public ProductManager(String pSessionName, String pDropShipSource) {
		setSessionName(pSessionName);
		setDropShipSource(pDropShipSource);
		// connection = java.sql.DriverManager.getConnection("jdbc:hsqldb:hsqlTempFiles;shutdown=true;", "sa", "");
		// String url = "jdbc:postgresql://localhost/amazon";
		// connection = java.sql.DriverManager.getConnection(url, "postgres", "postgres");

	}

	public boolean initDBConnection() {
		boolean success = false;
		String url = null;
		String userName = null;
		FileInputStream in = null;
		try {
			StandaloneConfiguration sc = StandaloneConfiguration.getInstance();
			url = sc.getDBURL();
			userName = sc.getDBUserName();
			String pswd = sc.getDBPassword();
			setDriverName(sc.getDBDriver());
			logger.debug("db.url: {}, db.username: {}, db.driver: {}", sc.getDBURL(), userName, getDriverName());
			Class.forName(getDriverName());
			logger.info("opening connection for database: {}", url);
			connection = java.sql.DriverManager.getConnection(url, userName, pswd);
			success = true;
			setConnectionEstablished(true);
		} catch (ClassNotFoundException e) {
			logger.error("Driver class not found for: " + DRIVER, e);
		} catch (SQLException e) {
			logger.error("Error getting connection for: db.url: " + url + ", db.username: " + userName + ", driver: "
					+ getDriverName(), e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logger.error("Error closing connection", e);
				}
			}
		}
		return success;
	}

	public void shutdownDB() {
		if (connection != null) {
			logger.info("Closing connection to database for session: {}", getSession());
			SSessionJdbc ses = getSession();
			ses.close();
		}
	}

	public void createTables() {
		SSessionJdbc ses = getSession();
		boolean success = false;
		try {
			ses.begin();
			for (SRecordMeta<?> table : TABLES) {
				ses.rawUpdateDB(ses.getDriver().createTableSQL(table));
				logger.info("Created table: {}", table.getTableName());
			}
			success = true;
		} finally {
			try {
				if (success) {
					ses.commit();
				} else {
					ses.rollback();
				}
			} catch (Exception e) {
				logger.error("error ending transaction", e);
			}
		}
	}

	public void dropTables() {
		SSessionJdbc ses = getSession();
		boolean success = false;
		try {
			ses.begin();
			for (SRecordMeta<?> table : TABLES) {
				String tableName = table.getTableName();
				ses.getDriver().dropTableNoError(tableName);
				logger.info("Drop table: {}", tableName);
			}
			success = true;
		} finally {
			try {
				if (success) {
					ses.commit();
				} else {
					ses.rollback();
				}
			} catch (Exception e) {
				logger.error("error ending transaction", e);
			}

		}
	}

	public SSessionJdbc getSession() {
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
			GetMatchingProductForId matchingProducts = new GetMatchingProductForId(productIds, mStartDate);
			JSONArray jsonArray = matchingProducts.matchProducts();
			insertJSONProducts(jsonArray, pUpdate);
		} else {
			logger.info("No products to process from list {}", pProductUPCList.toString());
		}
	}

	public boolean insertJSONProducts(JSONArray pJSONArray, boolean pUpdate) {
		boolean success = false;
		SSessionJdbc ses = null;
		try {
			ses = getSession();
			ses.begin();
			bulkInsert(pJSONArray, pUpdate);
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
		return success;
	}

	private List<String> filterExistingProducts(List<String> pProductUPCList, boolean pUpdate) {
		List<String> productIds = new ArrayList<String>();
		boolean success = false;
		SSessionJdbc ses = getSession();
		try {

			ses.begin();
			for (String upc : pProductUPCList) {
				if (!pUpdate) {
					SQuery<AmazonProductDAO> productQuery = new SQuery<AmazonProductDAO>(AmazonProductDAO.PRODUCT).eq(
							AmazonProductDAO.UPC, upc).eq(AmazonProductDAO.DROP_SHIP_SOURCE, getDropShipSource());
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
			success = true;
		} finally {
			if (success) {
				ses.commit();
			} else {
				ses.rollback();
			}
		}
		return productIds;
	}

	private void bulkInsert(JSONArray pJSONArray, boolean pUpdate) throws InsertException {
		logger.debug("pJSONArray: " + pJSONArray);

		int arraySize = pJSONArray.length();
		SSessionJdbc ses = getSession();
		// ses.setUseBatchUpdate(true);
		for (int i = 0; i < arraySize; i++) {
			JSONObject upcProducts = pJSONArray.getJSONObject(i);
			boolean amazonProductError = insertProductError(upcProducts);
			if (!amazonProductError) {
				JSONArray products = upcProducts.getJSONArray("products");
				Set<String> fieldNames = AmazonProductDAO.PRODUCT.getFieldNames();
				int size = products.length();
				for (int j = 0; j < size; j++) {
					JSONObject jsonProduct = products.getJSONObject(j);

					String marketPlaceId = jsonProduct.getString(MARKET_PLACE_ID);
					String asin = jsonProduct.getString(ASIN);
					AmazonProductDAO productRow = ses.findOrCreate(AmazonProductDAO.PRODUCT, marketPlaceId, asin);
					if (productRow.isNewRow() || pUpdate) {
						if (!productRow.isNewRow() && pUpdate) {
							logger.debug("Updating product: {}", asin);
						}
						String upc = upcProducts.getString(UPC);
						productRow.setString(AmazonProductDAO.UPC, upc);
						productRow.setString(AmazonProductDAO.STATUS, upcProducts.getString(STATUS));
						productRow.setString(AmazonProductDAO.ELASTICSEARCH_ID, marketPlaceId + "-" + asin);
						productRow.setString(AmazonProductDAO.DROP_SHIP_SOURCE, getDropShipSource());

						JSONArray prodNames = jsonProduct.names();
						int prodSize = prodNames.length();
						for (int k = 0; k < prodSize; k++) {

							String name = prodNames.getString(k);
							String nameUpper = name.toUpperCase();
							if (name.equalsIgnoreCase(ASIN) || name.equalsIgnoreCase(MARKET_PLACE_ID)) {
								// do not reset asin or marketplaceId, these represent primary keys of a product
								continue;
							} else if (name.equalsIgnoreCase(FEATURE)) {
								String feature = jsonProduct.getString(name);
								if (feature.length() > FEATURE_LENGTH) {
									// trim feature to FEATURE_LENGTH to match DB column length
									feature = feature.substring(0, FEATURE_LENGTH - 1);
								}

								productRow.setObject(productRow.getMeta().getField(nameUpper), feature);
							} else if (name.equalsIgnoreCase("ManufacturerPartsWarrantyDescription")) {
								productRow.setString(AmazonProductDAO.ManufacturerPartsWarrantyDescription,
										jsonProduct.getString(name));
							} else {
								try {
									if (fieldNames.contains(nameUpper)) {
										String value = jsonProduct.getString(name);
										SFieldMeta field = productRow.getMeta().getField(nameUpper);
										if (field instanceof SFieldString) {
											int maxLength = ((SFieldScalar) field).getMaxSize();
											if (value.length() > maxLength) {
												logger.warn(
														"ASIN: {}. Column: {}, size: {}, is to small for size: {}, value: {}",
														asin, name, maxLength, value.length(), value);
												value = value.substring(0, maxLength - 1);
											}

										}
										productRow.setObject(field, value);
									} else {
										logger.warn("Column name does not exist for: {}, value: {}, UPC: {}, ASIN: {}",
												nameUpper, jsonProduct.getString(name), upc, asin);
									}

								} catch (Exception e) {
									logger.error(
											"Error setting column on product UPC: {}, ASIN: {}: columnName: {}, columnValue: {}",
											upc, asin, name, jsonProduct.getString(name));
									throw new InsertException("Error inserting: " + name, e);
								}
							}
						}

					} else {
						logger.debug("Product should never exist at this point, must investigate {}", jsonProduct);
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
			productError.setString(AmazonProductErrorDAO.DROP_SHIP_SOURCE, getDropShipSource());
			productError.setObject(AmazonProductErrorDAO.JSON, upcProducts);
			productError.setString(AmazonProductErrorDAO.ERRORMESSAGE, upcProducts.getString("errorMessage"));
			productError.setString(AmazonProductErrorDAO.ERRORTYPE, upcProducts.getString("errorType"));
		}
		return isProductError;
	}

	/**
	 * Prints the query results returned by a SimpleDAO query.
	 */
	public void printQueryResults() {
		SSessionJdbc ses = getSession();
		ses.begin();
		SQueryResult<AmazonProductDAO> res = ses.query(new SQuery<AmazonProductDAO>(AmazonProductDAO.PRODUCT));
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

	public String getDriverName() {
		return mDriverName;
	}

	public void setDriverName(String pDriverName) {
		mDriverName = pDriverName;
	}

	public boolean isConnectionEstablished() {
		return mConnectionEstablished;
	}

	public void setConnectionEstablished(boolean pConnectionEstablished) {
		mConnectionEstablished = pConnectionEstablished;
	}

}
