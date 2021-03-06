package com.lokivog.mws.products;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simpleorm.dataset.SQuery;
import simpleorm.dataset.SRecordGeneric;
import simpleorm.dataset.SRecordInstance;
import simpleorm.sessionjdbc.SSessionJdbc;

import com.lokivog.mws.dao.AmazonProductDAO;
import com.lokivog.mws.dao.SellerProductDAO;

/**
 * The Class ProductQueryManager is a utility class for performing DAO queries.
 */
public class ProductQueryManager {

	final static Logger logger = LoggerFactory.getLogger(ProductQueryManager.class);
	private ProductManager mProductManager;

	public ProductQueryManager() {
		mProductManager = new ProductManager(ProductQueryManager.class.getSimpleName());
	}

	public ProductQueryManager(ProductManager pProductManager) {
		mProductManager = pProductManager;
	}

	public void shutdown() {
		getProductManager().shutdownDB();
	}

	public JSONObject queryProductType(String pUPC, int pPackageQuantity, String pSkuId, String pDropShipSource) {
		JSONObject jsonObject = null;
		logger.debug("queryProductType: UPC: {}, pPackageQuantity: {}, pSkuId: {}, pDropShipSource: {}", pUPC,
				pPackageQuantity, pSkuId, pDropShipSource);
		SSessionJdbc ses = getProductManager().getSession();
		try {
			ses.begin();
			SQuery productQuery = new SQuery<AmazonProductDAO>(AmazonProductDAO.PRODUCT).eq(AmazonProductDAO.UPC, pUPC)
					.eq(AmazonProductDAO.DROP_SHIP_SOURCE, pDropShipSource)
					.eq(AmazonProductDAO.PACKAGEQUANTITY, pPackageQuantity);
			List<SRecordInstance> products = ses.query(productQuery);
			if (!products.isEmpty()) {
				if (products.size() == 1) {
					jsonObject = new JSONObject();
					// investigate UPC 731015143139 with quantity 25, returns 2 products
					AmazonProductDAO product = (AmazonProductDAO) products.get(0);
					jsonObject.put("productGroup", product.getString(AmazonProductDAO.PRODUCTGROUP));
					jsonObject.put("asin", product.getString(AmazonProductDAO.ASIN));
					jsonObject.put("brand", product.getString(AmazonProductDAO.BRAND));
					jsonObject.put("upc", product.getString(AmazonProductDAO.UPC));
					jsonObject.put("model", product.getString(AmazonProductDAO.MODEL));
					jsonObject.put("productTypeName", product.getString(AmazonProductDAO.PRODUCTTYPENAME));
					jsonObject.put("marketPlaceId", product.getString(AmazonProductDAO.MARKETPLACEID));
				} else {
					logger.warn(
							"Found multiple products for UPC: {},  QUANTITY: {}, DROPSHIP: {}, products returned: {}, returning product with matching partnumber: {}. Must investigate",
							pUPC, pPackageQuantity, pDropShipSource, products.size(), pSkuId);

					for (SRecordInstance instance : products) {
						AmazonProductDAO product = (AmazonProductDAO) instance;
						logger.info("asin: " + product.getString(AmazonProductDAO.ASIN));
						if (pSkuId.equals(product.getString(AmazonProductDAO.PARTNUMBER))) {
							logger.info("asin: " + product.getString(AmazonProductDAO.ASIN));
							jsonObject = new JSONObject();
							jsonObject.put("productGroup", product.getString(AmazonProductDAO.PRODUCTGROUP));
							jsonObject.put("asin", product.getString(AmazonProductDAO.ASIN));
							jsonObject.put("brand", product.getString(AmazonProductDAO.BRAND));
							jsonObject.put("upc", product.getString(AmazonProductDAO.UPC));
							jsonObject.put("model", product.getString(AmazonProductDAO.MODEL));
							jsonObject.put("productTypeName", product.getString(AmazonProductDAO.PRODUCTTYPENAME));
							jsonObject.put("marketPlaceId", product.getString(AmazonProductDAO.MARKETPLACEID));
							break;
						}
					}
				}
			}
		} finally {
			if (ses != null) {
				ses.commit();
			}
		}
		return jsonObject;
	}

	public List<String> queryProductIdsToUpdate(Date pLastUpdateBy, String pIdType) {
		List<String> upcs = null;
		SSessionJdbc ses = getProductManager().getSession();
		try {
			String query = null;
			if (pIdType.equals("UPC")) {
				// query = "select distinct(upc) as upc from amz_product where last_updated < '2014-04-17 01:20:05.629' and batch_job < 4";
				query = "select distinct(upc) as upc from amz_product where batch_job < 4";
			} else if (pIdType.equals("ASIN")) {
				query = "select asin from amz_product where last_updated < '2014-04-19 01:20:05.629'";
			}
			if (query != null) {
				ses.begin();
				List<SRecordGeneric> results = ses.rawQuery(query, true);
				if (results != null && results.size() > 0) {
					upcs = new ArrayList<String>(results.size());
					for (SRecordGeneric record : results) {
						Iterator<String> iter = record.keySet().iterator();
						while (iter.hasNext()) {
							String key = iter.next();
							String value = (String) record.get(key);
							upcs.add(value);
						}
					}
				} else {
					upcs = null;
				}
			}
			// upcs.clear();
			// upcs.add("B00EYCKOLU");

		} finally {
			if (ses != null) {
				ses.commit();
			}
		}
		return upcs;
	}

	public AmazonProductDAO queryAmazonProduct(String pMarketPlaceId, String pASIN) {
		AmazonProductDAO product = null;
		SSessionJdbc ses = getProductManager().getSession();
		SQuery<AmazonProductDAO> productQuery = new SQuery<AmazonProductDAO>(AmazonProductDAO.PRODUCT).eq(
				AmazonProductDAO.MARKETPLACEID, pMarketPlaceId).eq(AmazonProductDAO.ASIN, pASIN);
		List<AmazonProductDAO> products = ses.query(productQuery);
		logger.debug("products returned from query: " + products);
		if (!products.isEmpty()) {
			if (products.size() > 1) {
				logger.info("returned multiple products for marketPlaceId: {}, ASIN: {}", pMarketPlaceId, pASIN);
			} else {
				product = products.get(0);
			}
		} else {
			logger.info("product not found for marketPlaceId: {}, ASIN: {}", pMarketPlaceId, pASIN);
		}
		return product;
	}

	public AmazonProductDAO queryAmazonProductByASIN(String pASIN) {
		AmazonProductDAO product = null;
		SSessionJdbc ses = getProductManager().getSession();
		SQuery<AmazonProductDAO> productQuery = new SQuery<AmazonProductDAO>(AmazonProductDAO.PRODUCT).eq(
				AmazonProductDAO.ASIN, pASIN);
		List<AmazonProductDAO> products = ses.query(productQuery);
		logger.debug("queryAmazonProductByASIN: products returned from query: " + products);
		if (!products.isEmpty()) {
			if (products.size() > 1) {
				logger.warn("queryAmazonProductByASIN: returned multiple products for ASIN: {}", pASIN);
			} else {
				product = products.get(0);
			}

		} else {
			logger.info("queryAmazonProductByASIN: product not found for ASIN: {}", pASIN);
		}
		return product;
	}

	public static void main(String[] args) {
		ProductQueryManager pqm = new ProductQueryManager();
		try {
			pqm.getKoleProducts();
		} finally {
			pqm.shutdown();
		}
	}

	public List<JSONObject> getKoleProducts() {
		List<JSONObject> products;
		SSessionJdbc ses = getProductManager().getSession();
		try {
			ses.begin();
			List<SRecordGeneric> results = ses
					.rawQuery(
							"select row_to_json(t) from (select brand,  upc, id as sku, inventory, item_weight as unit_weight, category, tier_pack_1, title, description from ds_kole_imports where inventory > 0 limit 1) t;",
							true);
			if (results != null && results.size() > 0) {
				products = new ArrayList<JSONObject>(results.size());
				for (SRecordGeneric record : results) {
					Iterator<String> iter = record.keySet().iterator();
					while (iter.hasNext()) {
						String key = iter.next();
						PGobject value = (PGobject) record.get(key);
						String jsonValue = value.getValue();
						JSONObject object = new JSONObject(jsonValue);
						products.add(object);
					}
				}
			} else {
				products = null;
			}

		} finally {
			if (ses != null) {
				ses.commit();
			}
		}
		return products;

	}

	public List<String> queryKoleProducts() {
		List<String> products;
		SSessionJdbc ses = getProductManager().getSession();
		try {
			ses.begin();
			List<SRecordGeneric> results = ses.rawQuery("select upc from ds_kole_imports where inventory > 0", true);
			if (results != null && results.size() > 0) {
				products = new ArrayList<String>(results.size());
				for (SRecordGeneric record : results) {
					Iterator<String> iter = record.keySet().iterator();
					while (iter.hasNext()) {
						String key = iter.next();
						String value = (String) record.get(key);
						products.add(value);
					}
				}
			} else {
				products = null;
			}

		} finally {
			if (ses != null) {
				ses.commit();
			}
		}
		return products;

	}

	public List<String> querySellerProducts() {
		boolean success = false;
		List<String> asinIds = null;
		SSessionJdbc ses = getProductManager().getSession();
		try {
			ses.begin();
			SQuery<SellerProductDAO> productQuery = new SQuery<SellerProductDAO>(SellerProductDAO.SELLER_PRODUCT);
			List<SellerProductDAO> products = ses.query(productQuery);
			asinIds = new ArrayList<String>(products.size());
			logger.debug("products returned from query: " + products);
			if (!products.isEmpty()) {
				for (SellerProductDAO product : products) {
					asinIds.add(product.getString(SellerProductDAO.ASIN));
				}
			} else {
				logger.info("seller products not found");
			}
		} finally {
			if (success) {
				ses.commit();
			} else {
				ses.rollback();
			}
		}
		return asinIds;
	}

	public void printQueryResults(List<SRecordGeneric> results) {
		if (results != null && results.size() > 0) {
			for (SRecordGeneric record : results) {
				Iterator<String> iter = record.keySet().iterator();
				while (iter.hasNext()) {
					String key = iter.next();
					Object value = record.get(key);
					logger.info("record key: {}, value: {}", key, value);
				}

			}
		}
	}

	public ProductManager getProductManager() {
		return mProductManager;
	}

	public void setProductManager(ProductManager pProductManager) {
		mProductManager = pProductManager;
	}

}
