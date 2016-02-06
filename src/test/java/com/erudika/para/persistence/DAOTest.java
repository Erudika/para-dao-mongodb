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

import com.erudika.para.core.App;
import com.erudika.para.core.utils.CoreUtils;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.search.Search;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import java.util.Arrays;
import java.util.Map;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.Mockito.*;
/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Ignore
public abstract class DAOTest {

	protected static DAO dao;
	protected static String appid1 = "testapp1";
	protected static String appid2 = "testapp2";

	private User u;
	private Tag t;

	@Before
	public void setUp() {
		CoreUtils.getInstance().setDao(dao);
		CoreUtils.getInstance().setSearch(mock(Search.class));
		u = new User(MongoDBUtils.generateNewId());
		u.setName("Name");
		u.setGroups(User.Groups.USERS.toString());
		u.setEmail("asd@asd.com");
		u.setIdentifier(u.getEmail());
		u.setPassword("123456");

		t = new Tag(MongoDBUtils.generateNewId());
		t.setCount(3);

		dao.create(u);
		dao.create(t);
	}

	@After
	public void tearDown() {
		dao.delete(u);
		dao.delete(t);
	}

	@Test
	public void testCreate() {
		assertNull(dao.create(null));
		assertEquals(u.getId(), dao.create(u));
		assertNotNull(u.getTimestamp());

		User x = dao.read(u.getId());
		assertEquals(u.getEmail(), x.getEmail());
		x.setEmail(null); // on test, this CAN be empty or null because @NotBlank will not be checked on testing
		assertNotNull(dao.create(x)); // this create doesn't create a new object but replace the old one because the same id
		x = dao.read(u.getId());
		assertNull(x.getEmail());

		// test multiapp support
		u.setId(u.getId()+"-APP1");
		u.setName("UserApp1");
		dao.create(appid1, u);
		assertEquals(appid1, dao.read(appid1, u.getId()).getAppid());
		assertNull(dao.read(u.getId()));
		assertNull(dao.read(appid2, u.getId()));

		t.setId(t.getId()+"-APP2");
		t.setName("TagApp2");
		dao.create(appid2, t);
		assertNotNull(dao.read(appid2, t.getId()));
		assertNull(dao.read(t.getId()));
		assertNull(dao.read(appid1, t.getId()));
		
		App app = new App("testappid");
		app.setName("testappid");
		app.setShared(false);
		app.create();
		App app2 = new App("testappid");
		assertTrue(app2.exists());
		
	}

	@Test
	public void testRead() {
		assertNull(dao.read(null));
		assertNull(dao.read("1"));
		assertNotNull(dao.read(u.getId()));
		assertEquals(u.getName(), dao.read(u.getId()).getName());
	}

	@Test
	public void testUpdate() {
		u.setName("Test Name");
		assertEquals(Utils.type(User.class), u.getType());
		dao.update(u);
		assertNotNull(u.getId());
		User x = dao.read(u.getId());
		assertEquals(u.getName(), x.getName());
		assertNotNull(x.getUpdated());

		// test updating locked fields
		App app = new App(MongoDBUtils.generateNewId());
		assertNotNull(dao.create(app));
		String secret = app.getSecret();
		assertNotNull(secret);
		app.resetSecret();
		dao.update(app);

		App app2 = dao.read(app.getId());
		assertNotNull(app2);
		assertEquals(secret, app2.getSecret());
	}

	@Test
	public void testDelete() {
		dao.delete(u);
		dao.delete(t);
		assertNull(dao.read(u.getId()));
		assertNull(dao.read(t.getId()));
	}

	@Test
	public void testCreateAllReadAllUpdateAllDeleteAll() {
		Sysprop t1 = new Sysprop(MongoDBUtils.generateNewId());
		Sysprop t2 = new Sysprop(MongoDBUtils.generateNewId());
		Sysprop t3 = new Sysprop(MongoDBUtils.generateNewId());

		// multi app support
		dao.createAll(appid1, Arrays.asList(t1, t2, t3));
		assertEquals(appid1, dao.read(appid1, t2.getId()).getAppid());
		assertNull(dao.read(t2.getId()));
		assertNull(dao.read(appid2, t2.getId()));

		dao.createAll(null);
		dao.createAll(Arrays.asList(t1, t2, t3));

		assertNotNull(t1.getId());
		assertNotNull(t2.getId());
		assertNotNull(t3.getId());
		assertNotNull(t1.getTimestamp());
		assertNotNull(t2.getTimestamp());
		assertNotNull(t3.getTimestamp());
		assertNotNull(dao.read(t1.getId()));
		assertNotNull(dao.read(t2.getId()));
		assertNotNull(dao.read(t3.getId()));

		dao.readAll(null, true);
		Map<String, Sysprop> props = dao.readAll(Arrays.asList(t1.getId(), t2.getId(), t3.getId()), true);

		assertFalse(props.isEmpty());
		assertTrue(props.containsKey(t1.getId()));
		assertTrue(props.containsKey(t2.getId()));
		assertTrue(props.containsKey(t3.getId()));

		assertTrue(t1.equals(props.get(t1.getId())));
		assertTrue(t2.equals(props.get(t2.getId())));
		assertTrue(t3.equals(props.get(t3.getId())));

		t1.setName("Name 1");
		t2.setName("Name 2");
		t3.setName("Name 3");

		t1.setType("type1");
		t2.setType("type2");
		t3.setType("type3");

		dao.updateAll(null);
		dao.updateAll(Arrays.asList(t1, t2, t3));

		assertNotNull(t1);
		assertNotNull(t2);
		assertNotNull(t3);

		assertNotNull(t1.getId());
		assertNotNull(t2.getId());
		assertNotNull(t3.getId());
		
		Sysprop tr1 = dao.read(t1.getId());
		Sysprop tr2 = dao.read(t2.getId());
		Sysprop tr3 = dao.read(t3.getId());

		assertNotNull(tr1);
		assertNotNull(tr2);
		assertNotNull(tr3);
				
		assertEquals(t1.getId(), tr1.getId());
		assertEquals(t2.getId(), tr2.getId());
		assertEquals(t3.getId(), tr3.getId());
		assertEquals(Utils.type(Sysprop.class), tr1.getType());
		assertEquals(Utils.type(Sysprop.class), tr2.getType());
		assertEquals(Utils.type(Sysprop.class), tr3.getType());
		assertEquals(t1.getName(), tr1.getName());
		assertEquals(t2.getName(), tr2.getName());
		assertEquals(t3.getName(), tr3.getName());
		assertNotNull(t1.getUpdated());
		assertNotNull(t2.getUpdated());
		assertNotNull(t3.getUpdated());

		dao.deleteAll(null);
		dao.deleteAll(Arrays.asList(t1, t2, t3));

		assertNull(dao.read(t1.getId()));
		assertNull(dao.read(t2.getId()));
		assertNull(dao.read(t3.getId()));

		Sysprop t4 = new Sysprop(MongoDBUtils.generateNewId());
		t4.setParentid(u.getId());
		dao.create(t4);
		assertNotNull(t4.getParentid());
		
		//try update locked fields
		t4.setParentid(t.getId()); 
		t4.setType("type4");
		dao.update(t4);
		assertNotNull(t4.getId());
		
		Sysprop tr4 = dao.read(t4.getId());
		assertEquals(Utils.type(Sysprop.class), tr4.getType());
		assertEquals(t4.getId(), tr4.getId());
		assertNotNull(tr4.getParentid());			
		assertEquals(u.getId(), tr4.getParentid());
	}

	@Test
	public void testReadPage() {
		// TODO
	}
}