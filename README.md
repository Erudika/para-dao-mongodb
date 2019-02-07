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

This plugin allows Para to store data in a MongoDB database.

## Documentation

### [Read the Docs](https://paraio.org/docs)

## Getting started

The plugin is on Maven Central. Here's the Maven snippet to include in your `pom.xml`:

```xml
<dependency>
  <groupId>com.erudika</groupId>
  <artifactId>para-dao-mongodb</artifactId>
  <version>{see_green_version_badge_above}</version>
</dependency>
```

Alternatively you can download the JAR from the "Releases" tab above put it in a `lib` folder alongside the server
WAR file `para-x.y.z.war`. Para will look for plugins inside `lib` and pick up the MongoDB plugin.

### Configuration

Here are all the configuration properties for this plugin (these go inside your `application.conf`):
```ini
# setting the URI will override host:port below
# URI is left blank by default
para.mongodb.uri = ""

para.mongodb.host = "localhost"
para.mongodb.port = 27017
para.mongodb.database = "MyApp"
para.mongodb.user = "user"
para.mongodb.password = "pass"
para.mongodb.ssl_enabled = false
para.mongodb.ssl_allow_all = false
```

You have the option to set either the server URI as a string (e.g. `mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]`) or set the 
host and port combination for a single server instance. The first option allows you to specify multiple server hosts.
If the URI has a non-blank value in the configuration file, it will override the host+port settings.
For detils about the server URI syntax, read the docs for [MongoClientURI](https://mongodb.github.io/mongo-java-driver/3.4/javadoc/com/mongodb/MongoClientURI.html).

Finally, set the config property:
```
para.dao = "MongoDBDAO"
```
This could be a Java system property or part of a `application.conf` file on the classpath.
This tells Para to use the MongoDB Data Access Object (DAO) implementation instead of the default.

### Field name limitation

Mongo enforces a restriction on all field names and does not allow `$` and `.` characters in field names.
This plugin tries to avoid this by encoding such fields in Base64, following the pattern
`Base64:{fieldName}:{encodedFieldName}`:
```
"$field.name" => "Base64:field_name:JGZpZWxkLm5hbWU="
```
The restricted characters are stripped and `.` is replaced with `_`.

### Dependencies

- MongoDB Java Driver for v3.4
- [Para Core](https://github.com/Erudika/para)

### Author

<a href="https://github.com/lucav">
<img src="https://avatars2.githubusercontent.com/u/795297?v=3&s=460" width="100" height="100">
</a>

#### Luca Venturella

## License
[Apache 2.0](LICENSE)
