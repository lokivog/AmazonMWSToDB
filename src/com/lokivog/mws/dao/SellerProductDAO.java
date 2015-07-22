package com.lokivog.mws.dao;

import simpleorm.dataset.SFieldBooleanChar;
import simpleorm.dataset.SFieldFlags;
import simpleorm.dataset.SFieldInteger;
import simpleorm.dataset.SFieldReference;
import simpleorm.dataset.SFieldScalar;
import simpleorm.dataset.SFieldString;
import simpleorm.dataset.SFieldTimestamp;
import simpleorm.dataset.SRecordInstance;
import simpleorm.dataset.SRecordMeta;

import com.lokivog.mws.Constants;

public class SellerProductDAO extends SRecordInstance {
	private static final long serialVersionUID = 1L;

	public static final SRecordMeta<SellerProductDAO> SELLER_PRODUCT = new SRecordMeta(SellerProductDAO.class,
			Constants.TABLE_SELLER_PRODUCT);

	// public static final SFieldString ID = new SFieldString(PRODUCT, "ID", 10, SPRIMARY_KEY);
	// public static final SFieldLong ID = (SFieldLong) new SFieldLong(SELLER_PRODUCT, "ID", SFieldFlags.SPRIMARY_KEY)
	// .setGeneratorMode(SSELECT_MAX, "product_seq"); // sequence tested too.

	// IMPORTANT DO NOT CHANGE THE ORDER OF THE PRIMARY KEYS IN THIS FILE
	// public static final SFieldString ID = new SFieldString(SELLER_PRODUCT, "ID", 60, SFieldFlags.SPRIMARY_KEY);
	public static final SFieldString MARKETPLACEID = new SFieldString(SELLER_PRODUCT, "MARKETPLACEID", 40,
			SFieldFlags.SPRIMARY_KEY);
	public static final SFieldString ASIN = new SFieldString(SELLER_PRODUCT, "ASIN", 40, SFieldFlags.SPRIMARY_KEY);
	static final SFieldReference<AmazonProductDAO> AMAZON_PRODUCT = new SFieldReference(SELLER_PRODUCT,
			AmazonProductDAO.PRODUCT, "AMAZON_PRODUCT", new SFieldScalar[] { MARKETPLACEID, ASIN }, new SFieldScalar[] {
					AmazonProductDAO.MARKETPLACEID, AmazonProductDAO.ASIN });
	// public static final SFieldString ASIN = new SFieldString(SELLER_PRODUCT, "ASIN", 40, SFieldFlags.SPRIMARY_KEY);
	public static final SFieldString DROP_SHIP_SOURCE = new SFieldString(SELLER_PRODUCT, "DROP_SHIP_SOURCE", 40);
	public static final SFieldString UPC = new SFieldString(SELLER_PRODUCT, "UPC", 40);
	public static final SFieldString DROPSHIP_ID = new SFieldString(SELLER_PRODUCT, "DROPSHIP_ID", 40);
	public static final SFieldInteger PACKAGEQUANTITY = new SFieldInteger(SELLER_PRODUCT, "PACKAGEQUANTITY");
	public static final SFieldInteger INVENTORY = new SFieldInteger(SELLER_PRODUCT, "INVENTORY");
	public static final SFieldBooleanChar ACTIVE = new SFieldBooleanChar(SELLER_PRODUCT, "ACTIVE", "true", "false");
	public static final SFieldTimestamp CREATION_DATE = new SFieldTimestamp(SELLER_PRODUCT, "CREATION_DATE");
	public static final SFieldTimestamp LAST_UPDATED = new SFieldTimestamp(SELLER_PRODUCT, "LAST_UPDATED");
	public static final SFieldTimestamp UPLOADED_DATE = new SFieldTimestamp(SELLER_PRODUCT, "UPLOADED_DATE");

	public @Override()
	SRecordMeta<SellerProductDAO> getMeta() {
		return SELLER_PRODUCT;
	};

}
