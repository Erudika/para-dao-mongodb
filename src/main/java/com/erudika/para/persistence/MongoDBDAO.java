/*
 * Copyright 2013-2016 Erudika. http://erudika.com
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
package com.erudika.para.persistence;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.erudika.para.annotations.Locked;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import java.util.ArrayList;
import java.util.Collections;

/**
 * MongoDB DAO implementation for Para.
 * @author Luca Venturella [lucaventurella@gmail.com]
 */
@Singleton
public class MongoDBDAO implements DAO {

	private static final Logger logger = LoggerFactory.getLogger(MongoDBDAO.class);
	private static final String _ID = "_id";
			
	public MongoDBDAO() { }

	/////////////////////////////////////////////
	//			CORE FUNCTIONS
	/////////////////////////////////////////////

	@Override
	public <P extends ParaObject> String create(String appid, P so) {
		if (so == null) {
			return null;
		}		
		
		if(StringUtils.isBlank(so.getId()) || !ObjectId.isValid(so.getId())){
			so.setId(MongoDBUtils.generateNewId());
			logger.debug("Generated id: " + so.getId());
		}
		if (so.getTimestamp() == null) {
			so.setTimestamp(Utils.timestamp());
		}
		so.setAppid(appid);
		createRow(so.getId(), appid, toRow(so, null));
		logger.debug("DAO.create() {}", so.getId());
		return so.getId();
	}

	@Override
	public <P extends ParaObject> P read(String appid, String key) {
		if (StringUtils.isBlank(key)) {
			return null;
		}
		P so = fromRow(readRow(key, appid));
		logger.debug("DAO.read() {} -> {}", key, so == null ? null : so.getType());
		return so != null ? so : null;
	}

	@Override
	public <P extends ParaObject> void update(String appid, P so) {
		if (so != null && so.getId() != null) {
			so.setUpdated(Utils.timestamp());
			updateRow(so.getId(), appid, toRow(so, Locked.class, true));
			logger.debug("DAO.update() {}", so.getId());
		}
	}

	@Override
	public <P extends ParaObject> void delete(String appid, P so) {
		if (so != null && so.getId() != null) {
			deleteRow(so.getId(), appid);
			logger.debug("DAO.delete() {}", so.getId());
		}
	}

	/////////////////////////////////////////////
	//				ROW FUNCTIONS
	/////////////////////////////////////////////

	private String createRow(String key, String appid, Document row) {
		if (StringUtils.isBlank(key) || StringUtils.isBlank(appid) || row == null || row.isEmpty()) {
			return null;
		}
		try {
			// if there isn't a document with the same id then create a new document 
			// else replace the document with the same id with the new one
			MongoDBUtils.getTable(appid).replaceOne(new Document(_ID, key), row, new UpdateOptions().upsert(true));			
		} catch (Exception e) {
			logger.error(null, e);
		}
		return key;
	}

	//http://www.mkyong.com/mongodb/java-mongodb-update-document/
	private void updateRow(String key, String appid, Document row) {
		if (StringUtils.isBlank(key) || StringUtils.isBlank(appid) || row == null || row.isEmpty()) {
			return;
		}
		try {
			UpdateResult u = MongoDBUtils.getTable(appid).updateOne(new Document(_ID, key), new Document("$set", row));
			logger.debug("key: " + key + " updated count: " + u.getModifiedCount());			
		} catch (Exception e) {
			logger.error(null, e);
		}
	}

	private Document readRow(String key, String appid) {
		if (StringUtils.isBlank(key) || StringUtils.isBlank(appid)) {
			return null;
		}
		Document row = null;
		try {
			row = MongoDBUtils.getTable(appid).find(new Document(_ID, key)).first();
			logger.debug("id: " + key + " row null: " + (row == null));
		} catch (Exception e) {
			logger.error(null, e);
		}
		return (row == null || row.isEmpty()) ? null : row;
	}

	private void deleteRow(String key, String appid) {
		if (StringUtils.isBlank(key) || StringUtils.isBlank(appid)) {
			return;
		}
		try {
			DeleteResult d = MongoDBUtils.getTable(appid).deleteOne(new Document(_ID, key));
			logger.debug("key: " + key + " deleted count: " + d.getDeletedCount());			
		} catch (Exception e) {
			logger.error(null, e);
		}
	}

	/////////////////////////////////////////////
	//				READ ALL FUNCTIONS
	/////////////////////////////////////////////

	@Override
	public <P extends ParaObject> void createAll(String appid, List<P> objects) {
		if (objects == null || objects.isEmpty() || StringUtils.isBlank(appid)) {
			return;
		}

		for (ParaObject object : objects) {
			create(appid, object);			
		}

		logger.debug("DAO.createAll() {}", (objects == null) ? 0 : objects.size());
	}

	@Override
	public <P extends ParaObject> Map<String, P> readAll(String appid, List<String> keys, boolean getAllColumns) {
		if (keys == null || keys.isEmpty() || StringUtils.isBlank(appid)) {
			return new LinkedHashMap<String, P>();
		}

		Map<String, P> results = new LinkedHashMap<String, P>(keys.size(), 0.75f, true);

		BasicDBObject inQuery = new BasicDBObject();
		inQuery.put(_ID, new BasicDBObject("$in", keys));

		MongoCursor<Document> cursor = MongoDBUtils.getTable(appid).find(inQuery).iterator();
		while(cursor.hasNext()) {
			Document d = cursor.next();
			P obj = fromRow(d);
			results.put(d.getString(_ID), obj);
		}

		logger.debug("DAO.readAll() {}", results.size());
		return results;
	}

	@Override
	public <P extends ParaObject> List<P> readPage(String appid, Pager pager) {
		// TODO: Stub!
		return Collections.emptyList();
	}

	@Override
	public <P extends ParaObject> void updateAll(String appid, List<P> objects) {
		if (objects != null) {
			for (P object : objects) {
				update(appid, object);
			}
		}
		logger.debug("DAO.updateAll() {}", (objects == null) ? 0 : objects.size());
	}

	@Override
	public <P extends ParaObject> void deleteAll(String appid, List<P> objects) {
		if (objects == null || objects.isEmpty() || StringUtils.isBlank(appid)) {
			return;
		}
		BasicDBObject query2 = new BasicDBObject();
		List<String> list = new ArrayList<String>();
		for (ParaObject object : objects) {
			list.add(object.getId());
		}
		query2.put(_ID, new BasicDBObject("$in", list));
		MongoDBUtils.getTable(appid).deleteMany(query2);
		logger.debug("DAO.deleteAll() {}", objects.size());
	}

	/////////////////////////////////////////////
	//				MISC FUNCTIONS
	/////////////////////////////////////////////

	private <P extends ParaObject> Document toRow(P so, Class<? extends Annotation> filter) {
		return toRow(so, filter, false);
	}
	
	private <P extends ParaObject> Document toRow(P so, Class<? extends Annotation> filter, boolean setNullFields) {
		Document row = new Document();
		if (so == null) {
			return row;
		}
		for (Entry<String, Object> entry : ParaObjectUtils.getAnnotatedFields(so, filter).entrySet()) {
			Object value = entry.getValue();
			if ((value != null && !StringUtils.isBlank(value.toString()))
					|| setNullFields) {	
				if(entry.getKey().equals(Config._ID)) // "id" in paraobject is translated to "_ID" mongodb
					row.put(_ID, value.toString());
				else
					row.put(entry.getKey(), value == null ? null : value.toString());
			}
		}
		return row;
	}

	private <P extends ParaObject> P fromRow(Document row) {
		if (row == null || row.isEmpty()) {
			logger.debug("row is null or empty");
			return null;
		}
		Map<String, Object> props = new HashMap<String, Object>();
		for (Entry<String, Object> col : row.entrySet()) {
			if(col.getKey().equals(_ID)) // "_ID" mongodb is translated to "id" in paraobject  
				props.put(Config._ID, col.getValue());
			else
				props.put(col.getKey(), col.getValue());
		}
		return ParaObjectUtils.setAnnotatedFields(props);
	}

	//////////////////////////////////////////////////////

	@Override
	public <P extends ParaObject> String create(P so) {
		return create(Config.APP_NAME_NS, so);
	}

	@Override
	public <P extends ParaObject> P read(String key) {
		return read(Config.APP_NAME_NS, key);
	}

	@Override
	public <P extends ParaObject> void update(P so) {
		update(Config.APP_NAME_NS, so);
	}

	@Override
	public <P extends ParaObject> void delete(P so) {
		delete(Config.APP_NAME_NS, so);
	}

	@Override
	public <P extends ParaObject> void createAll(List<P> objects) {
		createAll(Config.APP_NAME_NS, objects);
	}

	@Override
	public <P extends ParaObject> Map<String, P> readAll(List<String> keys, boolean getAllColumns) {
		return readAll(Config.APP_NAME_NS, keys, getAllColumns);
	}

	@Override
	public <P extends ParaObject> List<P> readPage(Pager pager) {
		return readPage(Config.APP_NAME_NS, pager);
	}

	@Override
	public <P extends ParaObject> void updateAll(List<P> objects) {
		updateAll(Config.APP_NAME_NS, objects);
	}

	@Override
	public <P extends ParaObject> void deleteAll(List<P> objects) {
		deleteAll(Config.APP_NAME_NS, objects);
	}

}
