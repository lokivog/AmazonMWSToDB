package com.lokivog.mws.products;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lokivog.mws.Constants;

import simpleorm.utils.SLog;

/**
 * The Class ProductScheduler is the main class to run for retrieving products from Amazon.
 */
public class ProductScheduler {

	final Logger logger = LoggerFactory.getLogger(ProductScheduler.class);

	public void testSplit() {
		List<String> ids = new ArrayList<String>();
		ids.add("731015110643");
		ids.add("731015110650");
		// ids.add("731015110704");
		// ids.add("731015110810");
		List<List> subLists = split(ids, 1);
		int count = 1;
		for (List subList : subLists) {
			logger.info("list: {}, size: {}", count++, subList.size());
		}
	}

	public void run() {
		ProductManager pm = null;
		try {
			pm = new ProductManager(ProductScheduler.class.getSimpleName(), Constants.DROP_SHIP_SOURCE_kOLE);
			SLog.getSessionlessLogger().setLevel(0);
			// SLog.setSlogClass(SLogSlf4j.class);

			pm.dropTables();
			pm.createTables();
			// pm.printQueryResults();
			List<String> ids = new ArrayList<String>();
			ids.add("73101511064323433");
			ids.add("731015110650");
			ids.add("731015110704");
			// ids.add("731015110810");
			boolean update = false;
			List<List<String>> subLists = split(ids, Constants.MAX_PRODUCT_LOOKUP_SIZE);
			for (List<String> subList : subLists) {
				pm.findAndInsertProducts(subList, update);
			}
			// ids.add("731015109036");

		} finally {
			if (pm != null) {
				pm.shutdownDB();
			}
		}

	}

	/**
	 * Splits a list into multiple sub lists.
	 * 
	 * @param list
	 *            the list
	 * @param i
	 *            the i
	 * @return the list
	 */
	private List split(List list, int i) {
		List<List<String>> out = new ArrayList<List<String>>();
		int size = list.size();
		int number = size / i;
		int remain = size % i;
		if (remain != 0) {
			number++;
		}
		for (int j = 0; j < number; j++) {
			int start = j * i;
			int end = start + i;
			if (end > list.size()) {
				end = list.size();
			}
			out.add(list.subList(start, end));
		}
		return out;
	}

	public static void main(String[] args) {
		ProductScheduler scheduler = new ProductScheduler();
		scheduler.run();
		// scheduler.testSplit();
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
