package com.lokivog.mws.products;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simpleorm.dataset.SQuery;
import simpleorm.dataset.SRecordInstance;
import simpleorm.sessionjdbc.SSessionJdbc;

import com.lokivog.mws.dao.AmazonProductDAO;

public class ProductsQueryManager {

	final static Logger logger = LoggerFactory.getLogger(ProductManager.class);

	public String queryProductType(ProductManager pProductManager, String pUPC, int pPackageQuantity,
			String pDropShipSource) {
		// getQueryText();
		String productGruop = null;
		SSessionJdbc ses = pProductManager.getSession();
		try {
			ses.begin();
			SQuery productQuery = new SQuery<AmazonProductDAO>(AmazonProductDAO.PRODUCT).eq(AmazonProductDAO.UPC, pUPC)
					.eq(AmazonProductDAO.DROP_SHIP_SOURCE, pDropShipSource)
					.eq(AmazonProductDAO.PACKAGEQUANTITY, pPackageQuantity);
			List<SRecordInstance> products = ses.query(productQuery);
			logger.info("Query for UPC: {}, PACKAGEQUANTITY: {}, result size: {}", pUPC, pPackageQuantity,
					products.size());
			if (!products.isEmpty()) {
				AmazonProductDAO product = (AmazonProductDAO) products.get(0);
				productGruop = product.getString(AmazonProductDAO.PRODUCTGROUP);
			}
			// printQueryResults(products);
		} finally {
			if (ses != null) {
				ses.commit();
			}
		}
		return productGruop;
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
}
