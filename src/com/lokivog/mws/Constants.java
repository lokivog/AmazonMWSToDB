package com.lokivog.mws;

public class Constants {
	// amazon product constants
	public static final String MARKET_PLACE_ID = "marketplaceId";
	public static final String ASIN = "asin";
	public static final String STATUS = "status";
	public static final String STATUS_SUCCESS = "Success";
	public static final String ES_ID = "elasticSearchId";
	public static final String UPC = "id";
	public static final String ID_TYPE = "idType";
	public static final String FEATURE = "Feature";
	public static final String PRODUCTS = "products";
	public static final int MAX_PRODUCT_LOOKUP_SIZE = 5;

	// other constants
	public static final String DROP_SHIP_SOURCE_DEFAULT = "default";
	public static final String DROP_SHIP_SOURCE_kOLE = "koleimports";
	public static final int FEATURE_LENGTH = 2555;
	public static final String OUTPUT_DIR = "output";
	public static final String OUTPUT_JSON_FILE = OUTPUT_DIR + "/amazonproducts.json";
	public static final String CONFIG_NAME = "resources/config.properties";

	// table constants
	public static final String TABLE_PRODUCT = "AMZ_PRODUCT";
	public static final String TABLE_PRODUCT_ERROR = "AMZ_PRODUCT_ERROR";
}
