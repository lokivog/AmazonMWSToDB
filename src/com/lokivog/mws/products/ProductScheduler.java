package com.lokivog.mws.products;

import java.util.ArrayList;
import java.util.List;

public class ProductScheduler {

	public static final String DROP_SHIP_SOURCE = "koleimports";

	public static void main(String[] args) {
		ProductManager pm = null;
		try {
			pm = new ProductManager();
			// pm.dropProductTable();
			// pm.createProductTable();
			// pm.printQueryResults();
			List<DropShipProduct> ids = new ArrayList<DropShipProduct>();

			ids.add(new DropShipProduct("603076897309", DROP_SHIP_SOURCE));
			// ids.add(new DropShipProduct("731015006625", DROP_SHIP_SOURCE));
			// ids.add(new DropShipProduct("731015006724", DROP_SHIP_SOURCE));
			// ids.add(new DropShipProduct("731015023950", DROP_SHIP_SOURCE));
			// ids.add(new DropShipProduct("731015004560", DROP_SHIP_SOURCE));
			pm.findAndInsertProducts(ids);
		} finally {
			if (pm != null) {
				pm.shutdownDB();
			}
		}
	}

	public List<String> getProductIds() {
		List<String> ids = new ArrayList<String>();
		ids.add("731015140640");
		ids.add("633040306001");
		ids.add("731015006403");
		ids.add("731015023950");
		ids.add("731015004560");
		// second 5
		ids.add("731015081684");
		ids.add("731015000012");
		ids.add("731015109357");
		ids.add("731015109111");
		ids.add("731015109289");
		// thrid 5
		ids.add("731015110643");
		ids.add("731015110650");
		ids.add("731015110704");
		ids.add("731015110810");
		ids.add("731015109036");
		ids.add("731015109302");
		ids.add("731015109524");
		ids.add("731015109777");
		ids.add("731015110001");
		ids.add("731015110032");
		ids.add("731015110056");
		ids.add("731015110063");
		ids.add("731015110070");
		ids.add("731015110582");
		ids.add("731015110476");
		ids.add("731015110797");
		ids.add("731015110803");
		ids.add("731015109456");
		ids.add("731015109463");
		return ids;
	}
}
