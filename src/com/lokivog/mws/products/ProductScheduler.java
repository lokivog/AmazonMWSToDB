package com.lokivog.mws.products;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class ProductScheduler is the main class to run for retrieving products from Amazon.
 */
public class ProductScheduler {

	final Logger logger = LoggerFactory.getLogger(ProductScheduler.class);

	public void syncDropBox() {
		Runtime r = Runtime.getRuntime();
		BufferedReader br = null;
		try {
			Process p = r.exec("scripts/sync_products_to_dropbox.sh");
			p.waitFor();
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";

			while ((line = br.readLine()) != null) {
				logger.info(line);
			}
		} catch (IOException e) {
			logger.error("IOException", e);
		} catch (InterruptedException e) {
			logger.error("InterruptedException", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("IOException", e);
				}
			}
		}
	}

	public void testlog() {
		logger.info("Amazon Response Error - Type: Sender, Code: InvalidParameterValue, Message: Invalid UPC identifier 70659812224 for marketplace ATVPDKIKX0DER");
	}

	public static void main(String[] args) {
		ProductScheduler scheduler = new ProductScheduler();
		// scheduler.syncDropBox();
		// scheduler.run(false, "UPC");

		// scheduler.installProductsFromDropBox();

		// ProcessFeed processFeed = new ProcessFeed();
		// processFeed.processFeed();
		// scheduler.testlog();

		// File file = new File("output/amazonproducts.json");
		// System.out.println(file.length() / 1024 / 1024);
		// scheduler.testSplit();
	}

}
