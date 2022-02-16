/*
 * Copyright 2013-2022 Erudika. http://erudika.com
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

module com.erudika.para.server.persistence.mongodb {
	requires com.erudika.para.core;
	requires org.apache.commons.lang3;
	requires org.mongodb.bson;
	requires org.slf4j;
	requires org.mongodb.driver.core;
	requires org.mongodb.driver.sync.client;
	exports com.erudika.para.server.persistence;
	provides com.erudika.para.core.persistence.DAO with com.erudika.para.server.persistence.MongoDBDAO;
}
