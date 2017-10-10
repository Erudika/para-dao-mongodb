/*
 * Copyright 2013-2017 Erudika. https://erudika.com
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

import com.erudika.para.core.Sysprop;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Luca Venturella [lucaventurella@gmail.com]
 */
public class MongoDBDAOIT extends DAOTest {

	private static final String ROOT_APP_NAME = "para-test";

	public MongoDBDAOIT() {
		super(new MongoDBDAO());
	}

	@BeforeClass
	public static void setUpClass() throws InterruptedException {
		System.setProperty("para.mongodb.port", "37017");
		System.setProperty("para.app_name", ROOT_APP_NAME);
		System.setProperty("para.cluster_name", ROOT_APP_NAME);
		MongoDBUtils.createTable(ROOT_APP_NAME);
		MongoDBUtils.createTable(appid1);
		MongoDBUtils.createTable(appid2);
		MongoDBUtils.createTable(appid3);
	}

	@AfterClass
	public static void tearDownClass() {
		MongoDBUtils.deleteTable(ROOT_APP_NAME);
		MongoDBUtils.deleteTable(appid1);
		MongoDBUtils.deleteTable(appid2);
		MongoDBUtils.deleteTable(appid3);
		MongoDBUtils.shutdownClient();
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

}
