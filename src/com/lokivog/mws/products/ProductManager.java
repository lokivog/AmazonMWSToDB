package com.lokivog.mws.products;

import static com.lokivog.mws.Constants.ASIN;
import static com.lokivog.mws.Constants.DROP_SHIP_SOURCE_DEFAULT;
import static com.lokivog.mws.Constants.FEATURE;
import static com.lokivog.mws.Constants.FEATURE_LENGTH;
import static com.lokivog.mws.Constants.ID;
import static com.lokivog.mws.Constants.ID_TYPE;
import static com.lokivog.mws.Constants.MARKET_PLACE_ID;
import static com.lokivog.mws.Constants.STATUS;
import static com.lokivog.mws.Constants.STATUS_SUCCESS;
import static com.lokivog.mws.Constants.UPC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
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
import simpleorm.dataset.SRecordInstance;
import simpleorm.dataset.SRecordMeta;
import simpleorm.sessionjdbc.SSessionJdbc;
import simpleorm.utils.SLog;

import com.lokivog.mws.Constants;
import com.lokivog.mws.dao.AmazonProductDAO;
import com.lokivog.mws.dao.AmazonProductErrorDAO;
import com.lokivog.mws.dao.DAOManager;
import com.lokivog.mws.dao.SellerProductDAO;

public class ProductManager extends DAOManager {

	public static final String DRIVER = "org.hsqldb.jdbcDriver";
	public static final String POSTGRES_DRIVER = "org.postgresql.Driver";

	public static final Set<String> IMMUTABLE_FIELDS = new HashSet<String>(Arrays.asList(ASIN, UPC, MARKET_PLACE_ID));

	public static final SRecordMeta<?>[] TABLES = { AmazonProductDAO.PRODUCT, AmazonProductErrorDAO.PRODUCT_ERROR,
			SellerProductDAO.SELLER_PRODUCT };
	final Logger logger = LoggerFactory.getLogger(ProductManager.class);

	private ProductQueryManager mProductQueryManager;

	/** The Drop ship source name to identify the dropship vendor the product originated from. */
	private String mDropShipSource;

	private Date mStartDate = new Date();

	private int mBatchNumber;

	/**
	 * Instantiates a new product manager with a name for the SimpleDAO session object. If used the dropShipSource is set to
	 * DROP_SHIP_SOURCE_DEFAULT by default.
	 * 
	 * @param pSessionName the session name
	 */
	public ProductManager(String pSessionName) {
		super(pSessionName);
		setDropShipSource(DROP_SHIP_SOURCE_DEFAULT);
		SLog.getSessionlessLogger().setLevel(0);
		mProductQueryManager = new ProductQueryManager(this);
	}

	/**
	 * Instantiates a new product manager with a name for the SimpleDAO session object and a name for the drop ship source.
	 * 
	 * @param pSessionName the session name
	 * @param pDropShipSource the drop ship source
	 */
	public ProductManager(String pSessionName, String pDropShipSource) {
		super(pSessionName);
		SLog.getSessionlessLogger().setLevel(0);
		setDropShipSource(pDropShipSource);
		mProductQueryManager = new ProductQueryManager(this);
	}

	public void findAndInsertProducts(List<String> pProductUPCList, boolean pUpdate, String pIdType) {
		List<String> productIds = filterExistingProducts(pProductUPCList, pUpdate);
		logger.debug("product ids: " + productIds);
		if (!productIds.isEmpty()) {
			GetMatchingProductForId matchingProducts = new GetMatchingProductForId(productIds, mStartDate, pIdType);
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
					logger.debug("insertJSONProducts: bulkInsert failed, rolling back transaction");
					ses.rollback();
				} else {
					logger.debug("insertJSONProducts: bulkInsert success, commiting transaction");
					ses.commit();
				}
			}
		}
		return success;
	}

	private List<String> filterExistingProducts(List<String> pProductUPCList, boolean pUpdate) {
		List<String> productIds = new ArrayList<String>(pProductUPCList.size());
		if (!pUpdate) {
			boolean success = false;
			SSessionJdbc ses = getSession();
			try {
				ses.begin();
				for (String upc : pProductUPCList) {
					SQuery<AmazonProductDAO> productQuery = new SQuery<AmazonProductDAO>(AmazonProductDAO.PRODUCT).eq(
							AmazonProductDAO.UPC, upc).eq(AmazonProductDAO.DROP_SHIP_SOURCE, getDropShipSource());
					List<AmazonProductDAO> products = ses.query(productQuery);
					logger.debug("products returned from query: " + products);
					if (products.isEmpty()) {
						productIds.add(upc);
					} else {
						logger.info("product already exist for upc: {}, source: {}", upc, getDropShipSource());
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
		} else {
			productIds.addAll(pProductUPCList);
		}
		return productIds;
	}

	private void bulkInsert(JSONArray pJSONArray, boolean pUpdate) throws InsertException {
		logger.debug("pJSONArray: " + pJSONArray);

		int arraySize = pJSONArray.length();
		SSessionJdbc ses = getSession();
		// ses.setUseBatchUpdate(true);
		Date now = new Date();
		for (int i = 0; i < arraySize; i++) {
			JSONObject upcProducts = pJSONArray.getJSONObject(i);
			String upcId = upcProducts.getString(ID);
			String idType = upcProducts.getString(ID_TYPE);
			String status = upcProducts.getString(STATUS);
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
					productRow.setInt(AmazonProductDAO.BATCH_JOB, getBatchNumber());
					boolean isNewRow = productRow.isNewRow();
					if (isNewRow || pUpdate) {

						if (isNewRow) {
							if (pUpdate) {
								logger.info("Found new ASIN: {} for Existing UPC: {}", asin, upcId);
							}
							productRow.setTimestamp(AmazonProductDAO.CREATION_DATE, now);
							productRow.setTimestamp(AmazonProductDAO.LAST_UPDATED, now);
							productRow.setString(AmazonProductDAO.DROP_SHIP_SOURCE, getDropShipSource());
							productRow.setString(AmazonProductDAO.ELASTICSEARCH_ID, marketPlaceId + "-" + asin);
							if (idType.equals(UPC)) {
								productRow.setString(AmazonProductDAO.UPC, upcId);
							}
						} else {
							if (idType.equalsIgnoreCase(ASIN)) {
								upcId = productRow.getString(AmazonProductDAO.UPC);
							}
							logger.debug("Updating product: {} with LAST_UPDATED: {}", asin, now);
							productRow.setTimestamp(AmazonProductDAO.LAST_UPDATED, now);
						}
						productRow.setString(AmazonProductDAO.STATUS, status);
						JSONArray prodNames = jsonProduct.names();

						int prodSize = prodNames.length();
						for (int k = 0; k < prodSize; k++) {

							String name = prodNames.getString(k);
							String nameUpper = name.toUpperCase();
							SFieldMeta field = productRow.getMeta().getField(nameUpper);
							Object jsonValue = jsonProduct.get(name);

							if (IMMUTABLE_FIELDS.contains(name)) {
								auditProductChanges(isNewRow, asin, upcId, jsonValue, name, productRow);
								// do not reset asin or marketplaceId, these represent primary keys of a product
								continue;
							} else if (name.equalsIgnoreCase(FEATURE)) {
								String feature = jsonProduct.getString(name);
								if (feature.length() > FEATURE_LENGTH) {
									// trim feature to FEATURE_LENGTH to match DB column length
									feature = feature.substring(0, FEATURE_LENGTH - 1);
								}
								if (auditProductChanges(isNewRow, asin, upcId, feature, name, productRow)) {
									productRow.setObject(field, feature);
								}
							} else if (name.equalsIgnoreCase("ManufacturerPartsWarrantyDescription")) {
								if (auditProductChanges(isNewRow, asin, upcId, jsonValue, "MANPARTSWARRANTYDESC",
										productRow)) {
									productRow.setString(AmazonProductDAO.ManufacturerPartsWarrantyDescription,
											jsonProduct.getString(name));
								}
							} else {
								try {
									if (fieldNames.contains(nameUpper)) {
										String value = jsonProduct.getString(name);
										if (field instanceof SFieldString) {
											int maxLength = ((SFieldScalar) field).getMaxSize();
											if (value.length() > maxLength) {
												logger.warn(
														"ASIN: {}. Column: {}, size: {}, is to small for size: {}, value: {}",
														asin, name, maxLength, value.length(), value);
												value = value.substring(0, maxLength - 1);
											}
										}
										if (auditProductChanges(isNewRow, asin, upcId, value, name, productRow)) {
											productRow.setObject(field, value);
										}
									} else {
										logger.warn("Column name does not exist for: {}, value: {}, UPC: {}, ASIN: {}",
												nameUpper, jsonProduct.getString(name), upcId, asin);
									}
								} catch (Exception e) {
									logger.error(
											"Error setting column on product UPC: {}, ASIN: {}: columnName: {}, columnValue: {}",
											upcId, asin, name, jsonProduct.getString(name));
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

	private boolean auditProductChanges(boolean pIsNewRow, String pASIN, String pUPC, Object pJSONValue, String pName,
			SRecordInstance pProductRow) {
		boolean hasChanged = false;
		if (pIsNewRow) {
			hasChanged = true;
			return hasChanged;
		}
		hasChanged = super.auditDAOChanges(pIsNewRow, pASIN, pJSONValue, pName, pProductRow);
		if (hasChanged) {
			String nameUpper = pName.toUpperCase();
			SFieldMeta daoField = pProductRow.getMeta().getField(nameUpper);
			if (daoField == null) {
				logger.info("Field: {} does not exist in Table", pName);
				return hasChanged;
			}
			Object daoValue = pProductRow.getObject(daoField);
			if (pName.equalsIgnoreCase("PACKAGEQUANTITY")) {
				logger.warn(
						"LOGTYPE:{}, WARNING PACKAGEQUANTITY WAS UPDATED!! Deactivate Product now. Field: {}, was updated: {}!={} old=new value: for product ASIN: {}, UPC: {}",
						"SELLER_UPDATE", nameUpper, daoValue, pJSONValue, pASIN, pUPC);
			} else {
				logger.info("LOGTYPE: {}, Field: {}, was updated, old/new: ({} != {}), for product ASIN: {} UPC: {}",
						"SELLER_UPDATE", pName, daoValue, pJSONValue, pASIN, pUPC);
			}
		}
		return hasChanged;
	}

	private boolean auditProductChanges(JSONObject pJsonProduct, AmazonProductDAO pProductRow) {
		boolean audit = true;
		String asin = pJsonProduct.getString(ASIN);
		logger.debug("auditing product ASIN: {} for changes ", asin);
		String upc = pProductRow.getString(AmazonProductDAO.UPC);
		JSONArray prodNames = pJsonProduct.names();
		int prodSize = prodNames.length();
		for (int k = 0; k < prodSize; k++) {
			boolean hasChanged = false;
			String name = prodNames.getString(k);
			String nameUpper = name.toUpperCase();
			Object jsonValue = pJsonProduct.getString(name);
			Object amazonValue = null;
			SFieldMeta field = pProductRow.getMeta().getField(nameUpper);
			if (field == null) {
				logger.info("Field: {} does not exist in Table", nameUpper);
				continue;
			} else {
				amazonValue = pProductRow.getObject(field);
			}
			if (jsonValue != null && amazonValue == null) {
				hasChanged = true;
			} else if (amazonValue instanceof String) {
				if (!jsonValue.equals(amazonValue)) {
					hasChanged = true;
				}
			} else if (amazonValue instanceof Integer) {
				Integer amazonInt = (Integer) amazonValue;
				Integer jsonInt = Integer.valueOf((String) jsonValue);
				if (!jsonInt.equals(amazonInt)) {
					if (nameUpper.equals("PACKAGEQUANTITY")) {
						logger.warn(
								"LOGTYPE: {}, WARNING PACKAGEQUANTITY WAS UPDATED!! Deactivate Product now. Field: {}, was updated: {}!={} old=new value: for product ASIN: {}, UPC: {}",
								"SELLER_UPDATE", name, amazonValue, jsonValue, asin, upc);
					}
					hasChanged = true;

				}
			} else if (amazonValue instanceof Double) {
				Double amazonDbl = (Double) amazonValue;
				Double jsonDbl = Double.valueOf((String) jsonValue);
				if (!jsonDbl.equals(amazonDbl)) {
					hasChanged = true;
				}
			} else if (amazonValue instanceof Boolean) {
				Boolean amazonBol = (Boolean) amazonValue;
				Boolean jsonBol = Boolean.valueOf((String) jsonValue);
				if (!jsonBol.equals(amazonBol)) {
					hasChanged = true;
				}
			} else {
				if (!jsonValue.equals(amazonValue)) {
					hasChanged = true;
					logger.info("jsonValue class: {}, value: {}", jsonValue.getClass(), jsonValue);
					logger.info("amazonValue class: {}, value: {}", amazonValue.getClass(), amazonValue);
				}
			}
			if (hasChanged) {
				logger.warn("LOGTYPE: {}, Field: {}, was updated, old/new: ({} != {}), for product ASIN: {}, UPC: {}",
						"SELLER_UPDATE", name, amazonValue, jsonValue, asin, upc);
			}
		}

		return audit;
	}

	public boolean insertProductError(JSONObject upcProducts) {
		boolean isProductError = false;
		String amazonStatus = upcProducts.getString(STATUS);
		if (!amazonStatus.equalsIgnoreCase(STATUS_SUCCESS)) {
			isProductError = true;
			String amazonId = upcProducts.getString(ID);
			String amazonIdType = upcProducts.getString(ID_TYPE);
			SSessionJdbc ses = getSession();
			AmazonProductErrorDAO productError = ses.createWithGeneratedKey(AmazonProductErrorDAO.PRODUCT_ERROR);
			productError.setString(AmazonProductErrorDAO.UPC, amazonId);
			productError.setString(AmazonProductErrorDAO.ID_TYPE, amazonIdType);
			productError.setString(AmazonProductErrorDAO.STATUS, amazonStatus);
			productError.setString(AmazonProductErrorDAO.DROP_SHIP_SOURCE, getDropShipSource());
			productError.setObject(AmazonProductErrorDAO.JSON, upcProducts);
			productError.setString(AmazonProductErrorDAO.ERRORMESSAGE, upcProducts.getString("errorMessage"));
			productError.setString(AmazonProductErrorDAO.ERRORTYPE, upcProducts.getString("errorType"));

			if (amazonIdType.equalsIgnoreCase(ASIN)) {
				AmazonProductDAO productRow = mProductQueryManager.queryAmazonProductByASIN(amazonId);
				if (productRow != null) {
					productRow.setString(AmazonProductDAO.STATUS, amazonStatus);
					productRow.setInt(AmazonProductDAO.BATCH_JOB, getBatchNumber());
					productRow.setDate(AmazonProductDAO.LAST_UPDATED, new Date());
				}
			}
		}
		return isProductError;
	}

	public boolean insertSellerProduct(List<AmazonProductDAO> pAmazonProducts, String pType) {
		boolean success = false;
		SSessionJdbc ses = getSession();
		try {
			ses.begin();
			for (AmazonProductDAO amazonProduct : pAmazonProducts) {
				String marketPlaceId = amazonProduct.getString(AmazonProductDAO.MARKETPLACEID);
				String asin = amazonProduct.getString(AmazonProductDAO.ASIN);
				String upc = amazonProduct.getString(AmazonProductDAO.UPC);
				Integer packageQauntity = amazonProduct.getInt(AmazonProductDAO.PACKAGEQUANTITY);
				SellerProductDAO sellerProduct = ses.findOrCreate(SellerProductDAO.SELLER_PRODUCT, marketPlaceId, asin);
				sellerProduct.setString(SellerProductDAO.UPC, upc);
				sellerProduct.setInt(SellerProductDAO.PACKAGEQUANTITY, packageQauntity);
				logger.info("adding seller product: {}", sellerProduct);
			}
			success = true;
		} catch (Exception e) {
			logger.error("Error adding seller products", e);
		} finally {
			if (ses != null) {
				if (success) {
					ses.commit();
				} else {
					ses.rollback();
				}
			}
		}

		return success;
	}

	public boolean insertSellerProduct(List<JSONObject> pJSONObjects) {
		boolean success = false;
		SSessionJdbc ses = getSession();
		Date now = new Date();
		try {
			ses.begin();
			logger.info("insertSellerProduct: inserting: {} products into table seller_product", pJSONObjects.size());
			for (JSONObject jsonObject : pJSONObjects) {
				String marketPlaceId = jsonObject.getString(MARKET_PLACE_ID);
				String asin = jsonObject.getString(ASIN);
				String upc = jsonObject.getString(UPC);
				int inventory = jsonObject.getInt("inventory");
				Integer packageQauntity = jsonObject.getInt(Constants.PACKAGE_QUANTITY);
				SellerProductDAO sellerProduct = ses.findOrCreate(SellerProductDAO.SELLER_PRODUCT, marketPlaceId, asin);
				sellerProduct.setString(SellerProductDAO.UPC, upc);
				sellerProduct.setInt(SellerProductDAO.PACKAGEQUANTITY, packageQauntity);
				sellerProduct.setInt(SellerProductDAO.INVENTORY, inventory);
				if (sellerProduct.isNewRow()) {
					sellerProduct.setDate(SellerProductDAO.CREATION_DATE, now);
				}
				sellerProduct.setDate(SellerProductDAO.LAST_UPDATED, now);
				sellerProduct.setDate(SellerProductDAO.UPLOADED_DATE, now);
				logger.debug("adding seller product: {}", sellerProduct);
			}
			success = true;
		} catch (Exception e) {
			logger.error("Error adding seller products", e);
		} finally {
			if (ses != null) {
				if (success) {
					ses.commit();
				} else {
					ses.rollback();
				}
			}
		}

		return success;
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

	public int getBatchNumber() {
		return mBatchNumber;
	}

	public void setBatchNumber(int pBatchNumber) {
		mBatchNumber = pBatchNumber;
	}
}
