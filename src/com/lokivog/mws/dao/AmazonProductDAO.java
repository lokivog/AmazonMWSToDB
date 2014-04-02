package com.lokivog.mws.dao;

import static com.lokivog.mws.Constants.FEATURE_LENGTH;
import static simpleorm.dataset.SFieldFlags.SDESCRIPTIVE;

import com.lokivog.mws.Constants;

import simpleorm.dataset.SFieldDouble;
import simpleorm.dataset.SFieldFlags;
import simpleorm.dataset.SFieldInteger;
import simpleorm.dataset.SFieldString;
import simpleorm.dataset.SRecordInstance;
import simpleorm.dataset.SRecordMeta;

public class AmazonProductDAO extends SRecordInstance {
	private static final long serialVersionUID = 1L;

	public static final SRecordMeta<AmazonProductDAO> PRODUCT = new SRecordMeta(AmazonProductDAO.class,
			Constants.TABLE_PRODUCT);

	// ie. SRecord objects describe SRecordInstances

	// public static final SFieldString ID = new SFieldString(PRODUCT, "ID", 10, SPRIMARY_KEY);
	// public static final SFieldLong ID = (SFieldLong) new SFieldLong(PRODUCT, "ID", SFieldFlags.SPRIMARY_KEY)
	// .setGeneratorMode(SSELECT_MAX, "product_seq"); // sequence tested too.

	// IMPORTANT DO NOT CHANGE THE ORDER OF THE PRIMARY KEYS IN THIS FILE
	public static final SFieldString MARKETPLACEID = new SFieldString(PRODUCT, "MARKETPLACEID", 40,
			SFieldFlags.SPRIMARY_KEY);
	public static final SFieldString ASIN = new SFieldString(PRODUCT, "ASIN", 40, SFieldFlags.SPRIMARY_KEY);
	public static final SFieldString ELASTICSEARCH_ID = new SFieldString(PRODUCT, "ELASTICSEARCH_ID", 60);

	public static final SFieldString DROP_SHIP_SOURCE = new SFieldString(PRODUCT, "DROP_SHIP_SOURCE", 40);
	public static final SFieldString UPC = new SFieldString(PRODUCT, "UPC", 40);
	public static final SFieldString STATUS = new SFieldString(PRODUCT, "STATUS", 40);
	public static final SFieldString BINDING = new SFieldString(PRODUCT, "BINDING", 40);
	public static final SFieldString SMALLIMAGE = new SFieldString(PRODUCT, "SMALLIMAGE", 255);
	public static final SFieldString FEATURE = new SFieldString(PRODUCT, "FEATURE", FEATURE_LENGTH);
	public static final SFieldString LISTPRICE = new SFieldString(PRODUCT, "LISTPRICE", 40);
	public static final SFieldInteger NUMBEROFITEMS = new SFieldInteger(PRODUCT, "NUMBEROFITEMS");

	public static final SFieldString MODEL = new SFieldString(PRODUCT, "MODEL", 40);
	public static final SFieldString SIZE = new SFieldString(PRODUCT, "SIZE", 40);
	public static final SFieldString BRAND = new SFieldString(PRODUCT, "BRAND", 40);
	public static final SFieldString PUBLISHER = new SFieldString(PRODUCT, "PUBLISHER", 40);
	public static final SFieldString PACKAGEDIMENSIONS = new SFieldString(PRODUCT, "PACKAGEDIMENSIONS", 40);
	public static final SFieldString ITEMDIMENSIONS = new SFieldString(PRODUCT, "ITEMDIMENSIONS", 40);
	public static final SFieldString STUDIO = new SFieldString(PRODUCT, "STUDIO", 40);
	public static final SFieldString LABEL = new SFieldString(PRODUCT, "LABEL", 40);
	public static final SFieldString MATERIALTYPE = new SFieldString(PRODUCT, "MATERIALTYPE", 40);
	public static final SFieldString PRODUCTGROUP = new SFieldString(PRODUCT, "PRODUCTGROUP", 40);
	public static final SFieldString COLOR = new SFieldString(PRODUCT, "COLOR", 40);
	public static final SFieldInteger PACKAGEQUANTITY = new SFieldInteger(PRODUCT, "PACKAGEQUANTITY");
	public static final SFieldString PARTNUMBER = new SFieldString(PRODUCT, "PARTNUMBER", 40);

	public static final SFieldString TITLE = new SFieldString(PRODUCT, "TITLE", 255, SDESCRIPTIVE);
	public static final SFieldString PRODUCTTYPENAME = new SFieldString(PRODUCT, "PRODUCTTYPENAME", 40);
	public static final SFieldString MANUFACTURER = new SFieldString(PRODUCT, "MANUFACTURER", 40);
	public static final SFieldDouble MANUFACTURERMINIMUMAGE = new SFieldDouble(PRODUCT, "MANUFACTURERMINIMUMAGE");

	public static final SFieldDouble PACKAGEWIDTH = new SFieldDouble(PRODUCT, "PACKAGEWIDTH");
	public static final SFieldDouble PACKAGELENGTH = new SFieldDouble(PRODUCT, "PACKAGELENGTH");
	public static final SFieldDouble PACKAGEWEIGHT = new SFieldDouble(PRODUCT, "PACKAGEWEIGHT");
	public static final SFieldDouble PACKAGEHEIGHT = new SFieldDouble(PRODUCT, "PACKAGEHEIGHT");

	public @Override()
	SRecordMeta<AmazonProductDAO> getMeta() {
		return PRODUCT;
	};
}
