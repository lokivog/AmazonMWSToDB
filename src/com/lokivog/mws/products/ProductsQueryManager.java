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

public class ProductsQueryManager {

	final static Logger logger = LoggerFactory.getLogger(ProductsQueryManager.class);
	private ProductManager mProductManager;

	public ProductsQueryManager() {
		mProductManager = new ProductManager(ProductsQueryManager.class.getSimpleName());
		mProductManager.initDBConnection();
	}

	public ProductsQueryManager(ProductManager pProductManager) {
		mProductManager = pProductManager;
		if (!mProductManager.isConnectionEstablished()) {
			mProductManager.initDBConnection();
		}
	}

	public void shutdown() {
		getProductManager().shutdownDB();
	}

	public JSONObject queryProductType(String pUPC, int pPackageQuantity, String pSkuId, String pDropShipSource) {
		// getQueryText();
		JSONObject jsonObject = null;
		SSessionJdbc ses = getProductManager().getSession();
		try {
			ses.begin();
			SQuery productQuery = new SQuery<AmazonProductDAO>(AmazonProductDAO.PRODUCT).eq(AmazonProductDAO.UPC, pUPC)
					.eq(AmazonProductDAO.DROP_SHIP_SOURCE, pDropShipSource)
					.eq(AmazonProductDAO.PACKAGEQUANTITY, pPackageQuantity);
			List<SRecordInstance> products = ses.query(productQuery);
			logger.info("Query for UPC: {}, PACKAGEQUANTITY: {}, result size: {}", pUPC, pPackageQuantity,
					products.size());
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
				} else {
					logger.warn(
							"Found multiple products for UPC: {},  QUANTITY: {}, DROPSHIP: {}, products returned: {}, returning product with matching partnumber: {}. Must investigate",
							pUPC, pPackageQuantity, pDropShipSource, products.size(), pSkuId);
					for (SRecordInstance instance : products) {
						AmazonProductDAO product = (AmazonProductDAO) instance;
						if (pSkuId.equals(product.getString(AmazonProductDAO.PARTNUMBER))) {
							jsonObject = new JSONObject();
							jsonObject.put("productGroup", product.getString(AmazonProductDAO.PRODUCTGROUP));
							jsonObject.put("asin", product.getString(AmazonProductDAO.ASIN));
							jsonObject.put("brand", product.getString(AmazonProductDAO.BRAND));
							jsonObject.put("upc", product.getString(AmazonProductDAO.UPC));
							jsonObject.put("model", product.getString(AmazonProductDAO.MODEL));
							jsonObject.put("productTypeName", product.getString(AmazonProductDAO.PRODUCTTYPENAME));
							break;
						}
					}

				}

			}
			// printQueryResults(products);
		} finally {
			if (ses != null) {
				ses.commit();
			}
		}
		return jsonObject;
	}

	public List<String> queryProductIdsToUpdate(Date pLastUpdateBy) {
		List<String> upcs;
		SSessionJdbc ses = getProductManager().getSession();
		try {
			ses.begin();
			List<SRecordGeneric> results = ses
					.rawQuery(
							"select distinct(upc) as upc from amz_product where last_updated < '2014-04-13 01:20:05.629'",
							true);
			// printQueryResults(results);
			if (results != null && results.size() > 0) {
				upcs = new ArrayList<String>(results.size());
				for (SRecordGeneric record : results) {
					Iterator<String> iter = record.keySet().iterator();
					while (iter.hasNext()) {
						String key = iter.next();
						String value = (String) record.get(key);
						upcs.add(value);

						// logger.info("tier_pack_1: {}", object.names());
					}

				}
			} else {
				upcs = null;
			}

		} finally {
			if (ses != null) {
				ses.commit();
			}
		}
		return upcs;
	}

	public static void main(String[] args) {
		ProductsQueryManager pqm = new ProductsQueryManager();
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
							"select row_to_json(t) from (select brand,  upc, id as sku, item_weight as unit_weight, category, tier_pack_1, title, description from kole_products where inventory > 0) t;",
							true);
			// printQueryResults(results);
			if (results != null && results.size() > 0) {
				products = new ArrayList<JSONObject>(results.size());
				for (SRecordGeneric record : results) {
					Iterator<String> iter = record.keySet().iterator();
					while (iter.hasNext()) {
						String key = iter.next();
						PGobject value = (PGobject) record.get(key);
						String jsonValue = value.getValue();
						logger.info("value: {}", jsonValue);
						JSONObject object = new JSONObject(jsonValue);
						logger.info("adding record value: {}", object);
						products.add(object);

						// logger.info("tier_pack_1: {}", object.names());
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
