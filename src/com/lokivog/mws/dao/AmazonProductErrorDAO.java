package com.lokivog.mws.dao;

import static simpleorm.dataset.SGeneratorMode.SSELECT_MAX;

import com.lokivog.mws.Constants;

import simpleorm.dataset.SFieldClob;
import simpleorm.dataset.SFieldFlags;
import simpleorm.dataset.SFieldLong;
import simpleorm.dataset.SFieldString;
import simpleorm.dataset.SRecordInstance;
import simpleorm.dataset.SRecordMeta;

public class AmazonProductErrorDAO extends SRecordInstance {
	private static final long serialVersionUID = 1L;

	public static final SRecordMeta<AmazonProductErrorDAO> PRODUCT_ERROR = new SRecordMeta(AmazonProductErrorDAO.class,
			Constants.TABLE_PRODUCT_ERROR);

	public static final SFieldLong ID = (SFieldLong) new SFieldLong(PRODUCT_ERROR, "ID", SFieldFlags.SPRIMARY_KEY)
			.setGeneratorMode(SSELECT_MAX, "product_seq"); // sequence tested too.

	// IMPORTANT DO NOT CHANGE THE ORDER OF THE PRIMARY KEYS IN THIS FILE

	public static final SFieldString DROP_SHIP_SOURCE = new SFieldString(PRODUCT_ERROR, "DROP_SHIP_SOURCE", 40);
	public static final SFieldString UPC = new SFieldString(PRODUCT_ERROR, "UPC", 40);
	public static final SFieldString ID_TYPE = new SFieldString(PRODUCT_ERROR, "ID_TYPE", 20);
	public static final SFieldString STATUS = new SFieldString(PRODUCT_ERROR, "STATUS", 40);
	public static final SFieldClob JSON = new SFieldClob(PRODUCT_ERROR, "JSON");

	public @Override()
	SRecordMeta<AmazonProductErrorDAO> getMeta() {
		return PRODUCT_ERROR;
	};
}
