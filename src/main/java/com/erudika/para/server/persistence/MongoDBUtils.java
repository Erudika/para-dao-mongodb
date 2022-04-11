/*
 * Copyright 2013-2022 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika
 */
package com.erudika.para.server.persistence;

import com.erudika.para.core.App;
import com.erudika.para.core.listeners.DestroyListener;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Para;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MongoDB DAO utilities for Para.
 * @author Luca Venturella [lucaventurella@gmail.com]
 */
public final class MongoDBUtils {

	private static final Logger logger = LoggerFactory.getLogger(MongoDBUtils.class);
	private static MongoClient mongodbClient;
	private static MongoDatabase mongodb;

	static {
		// Fix for exceptions from Spring Boot when using a different MongoDB host than localhost.
		System.setProperty("spring.autoconfigure.exclude",
				"org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
						+ "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration");
	}

	private MongoDBUtils() { }

	/**
	 * Returns a client instance for MongoDB.
	 * @return a client that talks to MongoDB
	 */
	public static MongoDatabase getClient() {
		if (mongodb != null) {
			return mongodb;
		}

		String dbUri = Para.getConfig().mongoConnectionUri();
		String dbHost = Para.getConfig().mongoHost();
		int dbPort = Para.getConfig().mongoPort();
		boolean sslEnabled = Para.getConfig().mongoSslEnabled();
		boolean sslAllowAll = Para.getConfig().mongoSslAllowAll();
		String dbName = Para.getConfig().mongoDatabase();
		String dbUser = Para.getConfig().mongoAuthUser();
		String dbPass = Para.getConfig().mongoAuthPassword();

		MongoClientSettings.Builder options = MongoClientSettings.builder().applyToSslSettings(b ->
				b.enabled(sslEnabled).invalidHostNameAllowed(sslAllowAll));

		if (!StringUtils.isBlank(dbUri)) {
			logger.info("MongoDB uri: " + dbUri.replaceAll("mongodb://.*@", "mongodb://<user:password>@") + ", database: " + dbName);
			options.applyConnectionString(new ConnectionString(dbUri));
			mongodbClient = MongoClients.create(options.build());
		} else {
			logger.info("MongoDB host: " + dbHost + ":" + dbPort + ", database: " + dbName);
			ServerAddress s = new ServerAddress(dbHost, dbPort);
			options.applyToClusterSettings(b -> b.hosts(Collections.singletonList(s)));
			if (!StringUtils.isBlank(dbUser) && !StringUtils.isBlank(dbPass)) {
				options.credential(MongoCredential.createCredential(dbUser, dbName, dbPass.toCharArray()));
			}
			mongodbClient = MongoClients.create(options.build());
		}

		mongodb = mongodbClient.getDatabase(dbName);

		if (!existsTable(Para.getConfig().getRootAppIdentifier())) {
			createTable(Para.getConfig().getRootAppIdentifier());
		}

		Para.addDestroyListener(new DestroyListener() {
			public void onDestroy() {
				shutdownClient();
			}
		});

		return mongodb;
	}

	/**
	 * Stops the client and releases resources.
	 * You can tell Para to call this on shutdown using {@code Para.addDestroyListener()}
	 */
	public static void shutdownClient() {
		if (mongodbClient != null) {
			mongodbClient.close();
			mongodbClient = null;
		}
	}

	/**
	 * Checks if the main table exists in the database.
	 * @param appid name of the {@link com.erudika.para.core.App}
	 * @return true if the table exists
	 */
	public static boolean existsTable(String appid) {
		if (StringUtils.isBlank(appid)) {
			return false;
		}
		try {
			appid = getTableNameForAppid(appid);
			MongoIterable<String> collectionNames = getClient().listCollectionNames();
			for (final String name : collectionNames) {
				if (name.equalsIgnoreCase(appid)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Creates a table in MongoDB.
	 * @param appid name of the {@link com.erudika.para.core.App}
	 * @return true if created
	 */
	public static boolean createTable(String appid) {
		if (StringUtils.isBlank(appid) || StringUtils.containsWhitespace(appid) || existsTable(appid)) {
			return false;
		}
		try {
			String table = getTableNameForAppid(appid);
			getClient().createCollection(table);
			// *** Don't need to create a secondary index here until when will be developed a full "Search" implementation for MongoDB ***
			// create a default seconday index for parentid field as string
			// getClient().getCollection(appid).createIndex(Indexes.text(Config._PARENTID));
			logger.info("Created MongoDB table '{}'.", table);
		} catch (Exception e) {
			logger.error(null, e);
			return false;
		}
		return true;
	}

	/**
	 * Deletes the main table from MongoDB.
	 * @param appid name of the {@link com.erudika.para.core.App}
	 * @return true if deleted
	 */
	public static boolean deleteTable(String appid) {
		if (StringUtils.isBlank(appid) || !existsTable(appid)) {
			return false;
		}
		try {
			MongoCollection<Document> collection = getTable(appid);
			if (collection != null) {
				collection.drop();
			}
			logger.info("Deleted MongoDB table '{}'.", getTableNameForAppid(appid));
		} catch (Exception e) {
			logger.error(null, e);
			return false;
		}
		return false;
	}

	/**
	 * Gives count information about a MongoDB table.
	 * @param appid name of the collection
	 * @return a long
	 */
	public static long getTableCount(final String appid) {
		if (StringUtils.isBlank(appid)) {
			return -1;
		}
		try {
			MongoCollection<Document> collection = getTable(appid);
			return (collection == null) ? 0 : collection.countDocuments();
		} catch (Exception e) {
			logger.error(null, e);
		}
		return -1;
	}

	/**
	 * Get the mongodb table requested.
	 * @param appid name of the collection
	 * @return a Mongo collection
	 */
	public static MongoCollection<Document> getTable(String appid) {
		try {
			return getClient().getCollection(getTableNameForAppid(appid));
		} catch (Exception e) {
			logger.error(null, e);
		}
		return null;
	}

	/**
	 * Lists all table names for this account.
	 * @return a list of MongoDB tables
	 */
	public static MongoIterable<String> listAllTables() {
		MongoIterable<String> collectionNames = getClient().listCollectionNames();
		return collectionNames;
	}

	/**
	 * Returns the table name for a given app id. Table names are usually in the form 'prefix-appid'.
	 * @param appIdentifier app id
	 * @return the table name
	 */
	public static String getTableNameForAppid(String appIdentifier) {
		if (StringUtils.isBlank(appIdentifier)) {
			return null;
		} else {
			return (App.isRoot(appIdentifier) || appIdentifier.startsWith(Config.PARA.concat("-"))) ?
					appIdentifier : Config.PARA + "-" + appIdentifier;
		}
	}

	/**
	 * Create a new unique objectid for MongoDB.
	 * @return the objectid as string
	 */
	public static String generateNewId() {
		return new ObjectId().toHexString();
	}
}
