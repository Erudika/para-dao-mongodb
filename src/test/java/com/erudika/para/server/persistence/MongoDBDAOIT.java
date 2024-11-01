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

import com.erudika.para.core.Sysprop;
import com.erudika.para.core.utils.Utils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Luca Venturella [lucaventurella@gmail.com]
 */
public class MongoDBDAOIT extends DAOTest {

	private static final String ROOT_APP_NAME = "para-test";
	private static TransitionWalker.ReachedState<RunningMongodProcess> running;
	private static MongoClient mongo;

	public MongoDBDAOIT() {
		super(new MongoDBDAO());
	}

	@BeforeAll
	public static void setUpClass() throws InterruptedException {
		running = Mongod.instance().start(Version.Main.V8_0);
		ServerAddress serverAddress = running.current().getServerAddress();
		mongo = MongoClients.create("mongodb://" + serverAddress);
//		MongoDatabase db = mongo.getDatabase("test");

		System.setProperty("para.mongodb.port", "" + serverAddress.getPort());
		System.setProperty("para.app_name", ROOT_APP_NAME);
		System.setProperty("para.cluster_name", ROOT_APP_NAME);

		MongoDBUtils.createTable(ROOT_APP_NAME);
		MongoDBUtils.createTable(appid1);
		MongoDBUtils.createTable(appid2);
		MongoDBUtils.createTable(appid3);
	}

	@AfterAll
	public static void tearDownClass() {
		MongoDBUtils.deleteTable(ROOT_APP_NAME);
		MongoDBUtils.deleteTable(appid1);
		MongoDBUtils.deleteTable(appid2);
		MongoDBUtils.deleteTable(appid3);
		MongoDBUtils.shutdownClient();

		mongo.close();
		running.current().stop();
//		running.close();
//		if (this.mongod != null) {
//			this.mongod.stop();
//			this.mongodExe.stop();
//		}
	}

	@Test
	public void testCreateDeleteExistsTable() throws InterruptedException {
		String testappid1 = "test-index";
		String badAppid = "test index 123";

		MongoDBUtils.createTable("");
		assertFalse(MongoDBUtils.existsTable(""));

		MongoDBUtils.createTable(testappid1);
		assertTrue(MongoDBUtils.existsTable(testappid1));

		MongoDBUtils.deleteTable(testappid1);
		assertFalse(MongoDBUtils.existsTable(testappid1));

		assertFalse(MongoDBUtils.createTable(badAppid));
		assertFalse(MongoDBUtils.existsTable(badAppid));
		assertFalse(MongoDBUtils.deleteTable(badAppid));
	}

	@Test
	public void testFieldNameSanitization() {
		MongoDBDAO d = ((MongoDBDAO) dao());
		assertNull(d.sanitizeField(null));
		assertTrue(d.sanitizeField("").isEmpty());
		assertTrue(d.sanitizeField("   KEY").equals("   KEY"));

		assertEquals("$$test.key.test...", d.desanitizeField(d.sanitizeField("$$test.key.test...")));

		assertEquals("Base64:test_key:JHRlc3Qua2V5", d.sanitizeField("$test.key"));
		assertEquals("$test.key", d.desanitizeField(d.sanitizeField("$test.key")));

		assertEquals("Base64:test_key:JCQkdGVzdC5rZXk=", d.sanitizeField("$$$test.key"));
		assertEquals("$$$test.key", d.desanitizeField(d.sanitizeField("$$$test.key")));

		assertEquals("Base64:test_key_two$:dGVzdC5rZXkudHdvJA==", d.sanitizeField("test.key.two$"));
		assertEquals("test.key.two$", d.desanitizeField(d.sanitizeField("test.key.two$")));

		assertEquals("test-key", d.sanitizeField("test-key"));
		assertEquals("test-key", d.desanitizeField(d.sanitizeField("test-key")));


		Sysprop s1 = new Sysprop("dirty-fields");
		s1.addProperty("$this.is.a.test", "test-value");
		s1.addProperty("$$$this...", 12345);
		s1.addProperty("$", "$");
		s1.addProperty(".", ".");

		d.create(s1);
//		Thread.sleep(200);
		Sysprop s2 = d.read(s1.getId());

		assertEquals("test-value", s2.getProperty("$this.is.a.test"));
		assertEquals(12345, s2.getProperty("$$$this..."));
		assertEquals("$", s2.getProperty("$"));
		assertEquals(".", s2.getProperty("."));

		s2.addProperty("$this.", "this");
		s2.addProperty("$$$this...", 54321);
		d.update(s2);

		s2 = d.read(s1.getId());
		assertEquals("test-value", s2.getProperty("$this.is.a.test"));
		assertEquals(54321, s2.getProperty("$$$this..."));
		assertEquals("this", s2.getProperty("$this."));
		assertEquals("$", s2.getProperty("$"));
		assertEquals(".", s2.getProperty("."));

		d.delete(s1);
	}

	@Test
	public void testInsertManyWithIdDuplication() {
		MongoDBDAO d = ((MongoDBDAO) dao());
		d.create(new Sysprop("id:three"));
		Sysprop s1 = new Sysprop("id:one");
		Sysprop s2 = new Sysprop("id:two");
		s2.setName("name1");
		Sysprop s3 = new Sysprop("id:two");
		s3.setName("name2");
		Sysprop s4 = new Sysprop("id:three");
		s4.setName("name4");
		Sysprop s5 = new Sysprop(Utils.getNewId());
		s5.setName("name5");
		s5.addProperty("_id", "id:three");
		s5.addProperty("id", "id:three");
		s5.addProperty("props", Collections.singletonMap("_id", "id:three"));
		Sysprop s6 = new Sysprop(Utils.getNewId());
		s6.setName("name6");
		s6.addProperty("_id", "id:three");
		s6.addProperty("id", "id:three");
		s6.addProperty("props", Collections.singletonMap("_id", "id:three"));

		d.createAll(List.of(s1, s2, s3, s4, s5, s6));

		assertNotNull(d.read(s1.getId()));
		assertNotNull(d.read(s2.getId()));
		assertNotNull(d.read(s3.getId()));
		assertNotNull(d.read(s4.getId()));
		assertNotNull(d.read(s5.getId()));

		s1.setName("updated1");
		s2.setName("updated2");
		s3.setName("updated3");
		s4.setName("updated4");
		s5.setName("updated5");

		d.updateAll(List.of(s1, s2, s3, s4, s5));

		assertEquals("updated1", d.read(s1.getId()).getName());
		assertEquals("updated3", d.read(s2.getId()).getName());
		assertEquals("updated3", d.read(s3.getId()).getName());
		assertEquals("updated4", d.read(s4.getId()).getName());
		assertEquals("updated5", d.read(s5.getId()).getName());

		d.deleteAll(List.of(s1, s2, s3, s4, s5, s6));
	}

}
