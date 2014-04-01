package com.lokivog.mws.products;

public class DropShipProduct {

	private String mUPC;
	private String mSource;

	public DropShipProduct() {

	}

	public DropShipProduct(String pUPC, String pSource) {
		super();
		mUPC = pUPC;
		mSource = pSource;
	}

	public String getUPC() {
		return mUPC;
	}

	public void setUPC(String pUPC) {
		mUPC = pUPC;
	}

	public String getSource() {
		return mSource;
	}

	public void setSource(String pSource) {
		mSource = pSource;
	}

}
