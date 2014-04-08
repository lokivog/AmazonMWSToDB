package com.lokivog.mws.products;

import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simpleorm.dataset.SQuery;
import simpleorm.dataset.SRecordInstance;
import simpleorm.sessionjdbc.SSessionJdbc;

import com.lokivog.mws.dao.AmazonProductDAO;

public class ProductsQueryManager {

	final static Logger logger = LoggerFactory.getLogger(ProductManager.class);
	private ProductManager mProductManager = new ProductManager(ProductsQueryManager.class.getSimpleName());

	public ProductsQueryManager() {
		mProductManager.initDBConnection();
	}

	public void shutdown() {
		getProductManager().shutdownDB();
	}

	public JSONObject queryProductType(String pUPC, int pPackageQuantity, String pDropShipSource) {
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
					AmazonProductDAO product = (AmazonProductDAO) products.get(0);
					jsonObject.put("productGroup", product.getString(AmazonProductDAO.PRODUCTGROUP));
					jsonObject.put("asin", product.getString(AmazonProductDAO.ASIN));
				} else {
					logger.error(
							"Found multiple products for UPC: {}, QUANTITY: {}, DROPSHIP: {}, products returned: {}, not returning either any product. Must investigate",
							pUPC, pPackageQuantity, pDropShipSource, products.size());
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

	public void printQueryResults(List<SRecordInstance> pResults) {
		for (SRecordInstance product : pResults) {
			logger.info("----------- New Product ------------");
			Set<String> fieldNames = product.getMeta().getFieldNames();
			for (String fieldName : fieldNames) {
				logger.info(fieldName + ": " + product.getObject(product.getMeta().getField(fieldName)));
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
