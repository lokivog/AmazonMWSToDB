package com.lokivog.mws.config;

import java.util.Properties;

import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lokivog.mws.products.GetMatchingProductForId;

public final class ConfigurationLoader {
	private static final Logger logger = LoggerFactory.getLogger(GetMatchingProductForId.class);
	private static ConfigurationLoader instance = new ConfigurationLoader();

	private ConfigurationLoader() {
	}

	public static ConfigurationLoader getInstance() {
		return instance;
	}

	public Properties loadProperties(String fileName) {
		try {
			return ConfigurationConverter.getProperties(loadPropertiesConfiguration(fileName));
		} catch (org.apache.commons.configuration.ConfigurationException ex) {
			logger.error("ConfigurationException", ex);
			return null;
		}
	}

	public PropertiesConfiguration loadPropertiesConfiguration(String fileName)
			throws org.apache.commons.configuration.ConfigurationException {
		return new PropertiesConfiguration(fileName);
	}
}
