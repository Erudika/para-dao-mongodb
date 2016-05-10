![Logo](https://s3-eu-west-1.amazonaws.com/org.paraio/para.png)
============================

> ### MongoDB DAO plugin for Para

[![Build Status](https://travis-ci.org/Erudika/para-dao-mongodb.svg?branch=master)](https://travis-ci.org/Erudika/para-dao-mongodb)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.erudika/para-dao-mongodb/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.erudika/para-dao-mongodb)
[![Join the chat at https://gitter.im/Erudika/para](https://badges.gitter.im/Erudika/para.svg)](https://gitter.im/Erudika/para?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## What is this?

**Para** was designed as a simple and modular back-end framework for object persistence and retrieval.
It enables your application to store objects directly to a data store (NoSQL) or any relational database (RDBMS)
and it also automatically indexes those objects and makes them searchable.

## Documentation

### [Read the Docs](http://paraio.org/docs)

## Getting started

The plugin is on Maven Central. Here's the Maven snippet to include in your `pom.xml`:

```xml
<dependency>
  <groupId>com.erudika</groupId>
  <artifactId>para-dao-mongodb</artifactId>
  <version>1.18.4</version>
</dependency>
```

Add the project as dependency through Maven and set the config property
```
para.dao = "MongoDBDAO"
```
This could be a Java system property or part of a `application.conf` file on the classpath.
This tells Para to use the MongoDB Data Access Object (DAO) implementation instead of the default.


Alternatively you can build the project with `mvn clean install` and unzip the file `target/para-dao-mongodb.zip`
into a `lib` folder alongside the server WAR file `para-server.war`. Para will look for plugins inside `lib`
and pick up the MongoDB plugin.

Finally, make sure you close the client in your code on exit:
```java
Para.addDestroyListener(new DestroyListener() {
	public void onDestroy() {
		MongoDBUtils.shutdownClient();
	}
});
```

### Requirements

- MongoDB Java Driver for v3.2
- [Para Core](https://github.com/Erudika/para)

### Author

<a href="https://github.com/lucav">
<img src="https://avatars2.githubusercontent.com/u/795297?v=3&s=460" width="100" height="100">
</a>

#### Luca Venturella

## License
[Apache 2.0](LICENSE)
