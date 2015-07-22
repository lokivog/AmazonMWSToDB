package com.lokivog.mws.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simpleorm.dataset.SFieldMeta;
import simpleorm.dataset.SRecordInstance;
import simpleorm.dataset.SRecordMeta;
import simpleorm.sessionjdbc.SSessionJdbc;

import com.lokivog.mws.config.StandaloneConfiguration;

public class DAOManager {

	final Logger logger = LoggerFactory.getLogger(DAOManager.class);

	private String mSessionName;
	private Connection mConnection = null;
	private boolean mConnectionEstablished = false;
	private String mDriverName;

	/**
	 * Instantiates a new product manager with a name for the SimpleDAO session object. If used the dropShipSource is set to
	 * DROP_SHIP_SOURCE_DEFAULT by default.
	 * 
	 * @param pSessionName the session name
	 */
	public DAOManager(String pSessionName) {
		setSessionName(pSessionName);
	}

	public boolean initDBConnection() {
		boolean success = false;
		String url = null;
		String userName = null;
		FileInputStream in = null;
		try {
			StandaloneConfiguration sc = StandaloneConfiguration.getInstance();
			url = sc.getDBURL();
			userName = sc.getDBUserName();
			String pswd = sc.getDBPassword();
			setDriverName(sc.getDBDriver());
			logger.debug("db.url: {}, db.username: {}, db.driver: {}", sc.getDBURL(), userName, getDriverName());
			Class.forName(getDriverName());
			logger.info("opening connection for database: {}", url);
			mConnection = java.sql.DriverManager.getConnection(url, userName, pswd);
			success = true;
			setConnectionEstablished(true);
		} catch (ClassNotFoundException e) {
			logger.error("Driver class not found for: " + getDriverName(), e);
		} catch (SQLException e) {
			logger.error("Error getting connection for: db.url: " + url + ", db.username: " + userName + ", driver: "
					+ getDriverName(), e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logger.error("Error closing connection", e);
				}
			}
		}
		return success;
	}

	public void shutdownDB() {
		if (mConnection != null) {
			logger.info("Closing connection to database for session: {}", getSession());
			SSessionJdbc ses = getSession();
			ses.close();
		}
	}

	public void createTables(SRecordMeta<?>[] pTables) {
		SSessionJdbc ses = getSession();
		boolean success = false;
		try {
			ses.begin();
			for (SRecordMeta<?> table : pTables) {
				ses.rawUpdateDB(ses.getDriver().createTableSQL(table));
				logger.info("Created table: {}", table.getTableName());
			}
			success = true;
		} finally {
			try {
				if (success) {
					ses.commit();
				} else {
					ses.rollback();
				}
			} catch (Exception e) {
				logger.error("error ending transaction", e);
			}
		}
	}

	public void createTables(SRecordMeta<?> pTable) {
		SSessionJdbc ses = getSession();
		boolean success = false;
		try {
			ses.begin();
			ses.rawUpdateDB(ses.getDriver().createTableSQL(pTable));
			logger.info("Created table: {}", pTable.getTableName());
			success = true;
		} finally {
			try {
				if (success) {
					ses.commit();
				} else {
					ses.rollback();
				}
			} catch (Exception e) {
				logger.error("error ending transaction", e);
			}
		}
	}

	public void dropTables(SRecordMeta<?>[] pTables) {
		SSessionJdbc ses = getSession();
		boolean success = false;
		try {
			ses.begin();
			for (SRecordMeta<?> table : pTables) {
				String tableName = table.getTableName();
				ses.getDriver().dropTableNoError(tableName);
				logger.info("Drop table: {}", tableName);
			}
			success = true;
		} finally {
			try {
				if (success) {
					ses.commit();
				} else {
					ses.rollback();
				}
			} catch (Exception e) {
				logger.error("error ending transaction", e);
			}

		}
	}

	public void dropTables(SRecordMeta<?> pTable) {
		SSessionJdbc ses = getSession();
		boolean success = false;
		try {
			ses.begin();
			String tableName = pTable.getTableName();
			ses.getDriver().dropTableNoError(tableName);
			logger.info("Drop table: {}", tableName);
			success = true;
		} finally {
			try {
				if (success) {
					ses.commit();
				} else {
					ses.rollback();
				}
			} catch (Exception e) {
				logger.error("error ending transaction", e);
			}

		}
	}

	public SSessionJdbc getSession() {
		SSessionJdbc ses = SSessionJdbc.getThreadLocalSession();
		if (ses == null) {
			ses = SSessionJdbc.open(mConnection, getSessionName());
		}
		return ses;
	}

	public boolean auditDAOChanges(boolean pIsNewRow, String pId, Object pValue, String pName,
			SRecordInstance pProductRow) {
		boolean hasChanged = false;
		if (pIsNewRow) {
			hasChanged = true;
			return hasChanged;
		}
		String nameUpper = pName.toUpperCase();
		Object daoValue = null;
		SFieldMeta daoField = pProductRow.getMeta().getField(nameUpper);
		if (daoField == null) {
			logger.info("Field: {} does not exist in Table", nameUpper);
			return hasChanged;
		} else {
			daoValue = pProductRow.getObject(daoField);
		}
		if (pValue != null && daoValue == null) {
			hasChanged = true;
		} else if (daoValue instanceof String) {
			if (!pValue.equals(daoValue)) {
				hasChanged = true;
			}
		} else if (daoValue instanceof Integer) {
			Integer amazonInt = (Integer) daoValue;
			Integer jsonInt = Integer.valueOf((String) pValue);
			if (!jsonInt.equals(amazonInt)) {
				hasChanged = true;
			}
		} else if (daoValue instanceof Double) {
			Double amazonDbl = (Double) daoValue;
			Double jsonDbl = Double.valueOf((String) pValue);
			if (!jsonDbl.equals(amazonDbl)) {
				hasChanged = true;
			}
		} else if (daoValue instanceof Boolean) {
			Boolean amazonBol = (Boolean) daoValue;
			Boolean jsonBol = Boolean.valueOf((String) pValue);
			if (!jsonBol.equals(amazonBol)) {
				hasChanged = true;
			}
		} else if (daoValue instanceof java.sql.Date && pValue instanceof String) {
			java.sql.Date doaValueDate = (java.sql.Date) daoValue;
			String pValueStr = (String) pValue;
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			try {
				Date parsed = format.parse(pValueStr);
				java.sql.Date sql = new java.sql.Date(parsed.getTime());
				if (sql.compareTo(doaValueDate) != 0) {
					hasChanged = true;
				}
			} catch (ParseException e) {
				logger.error("ParseException parsing: {} ID: {}", pValueStr, pId, e);
			}

		} else {
			if (!pValue.equals(daoValue)) {
				hasChanged = true;
				logger.info("Value class: {}, value: {}", pValue.getClass(), pValue);
				logger.info("DAO class: {}, value: {}", daoValue.getClass(), daoValue);
			}
		}
		if (!hasChanged) {
			logger.debug("auditProductChanges: no changes for ASIN: {}", pId);
		}
		return hasChanged;
	}

	public String getSessionName() {
		return mSessionName;
	}

	public void setSessionName(String pSessionName) {
		mSessionName = pSessionName;
	}

	public String getDriverName() {
		return mDriverName;
	}

	public void setDriverName(String pDriverName) {
		mDriverName = pDriverName;
	}

	public boolean isConnectionEstablished() {
		return mConnectionEstablished;
	}

	public void setConnectionEstablished(boolean pConnectionEstablished) {
		mConnectionEstablished = pConnectionEstablished;
	}

}
