package com.lokivog.mws.config;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for running the application as a standalone java process.
 * 
 * @author mvogel
 * 
 */
public class StandaloneConfiguration {
	public static final String CONFIGURATION_PROPERTY_FILE_NAME = "/home/mvogel/github/AmazonMWSToDB/resources/standalone.configuration.properties";
	private static final Logger logger = LoggerFactory.getLogger(StandaloneConfiguration.class);
	private PropertiesConfiguration propertiesConfiguration;
	private static StandaloneConfiguration instance = new StandaloneConfiguration();

	protected StandaloneConfiguration() {
		loadConfiguration();
	}

	public static void setInstance(StandaloneConfiguration instance) {
		StandaloneConfiguration.instance = instance;
	}

	/**
	 * return the single instance
	 * 
	 * @return
	 */
	public static StandaloneConfiguration getInstance() {
		return instance;
	}

	/**
	 * load configuration data
	 */
	protected void loadConfiguration0() {
		try {
			propertiesConfiguration = ConfigurationLoader.getInstance().loadPropertiesConfiguration(
					CONFIGURATION_PROPERTY_FILE_NAME);
		} catch (org.apache.commons.configuration.ConfigurationException ex) {
			logger.error("ConfigurationException", ex);
		}
	}

	private void loadConfiguration() {
		loadConfiguration0();
	}

	public String getDBURL() {
		return propertiesConfiguration.getString("db.url");
	}

	public String getDBUserName() {
		return propertiesConfiguration.getString("db.username");
	}

	public String getDBPassword() {
		return propertiesConfiguration.getString("db.password");
	}

	public String getDBDriver() {
		return propertiesConfiguration.getString("db.driver");
	}

	public String getBuildProductsDir() {
		return propertiesConfiguration.getString("build.products.dir");
	}

}
