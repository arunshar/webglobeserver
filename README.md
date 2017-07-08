# webglobeserver

## Installing webglobeserver

### Step 1 - Install Tomcat Server
Follow directions at http://tomcat.apache.org/. We will assume that the base Tomcat directory is at: `mytomcathome`. We will also assume that the base Tomcat URL is `mytomcaturl`
### Step 2 - Install MySQL
Follow directions at https://www.mysql.com/downloads/. We will assume that `mysqluser` and `mysqlpassword` are chosen as the username and password for the mysql user account.
### Step 3 - Setup database
From the command line:
```
cd webglobeserver
mysql -u mysqlusername -pmysqlpassword < setup/webglobeserver.dump.sql
```
### Step 4 - Install ant
Follow directions at http://ant.apache.org/
### Step 5 - Install Hadoop Distributed File System (HDFS)
Follow directions at https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/SingleCluster.html for single node or https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/ClusterSetup.html multi-node installation.

We will assume that `myhdfsurl` is the base HDFS url and `myhdfsbasedir` is a writable directory within the HDFS where the data will be stored. Additionally, we will assume that `mylocaltmpdir` is a writable directory (e.g., `/tmp`) on the local file system.
### Step 6 - Setup parameters
In `WEB-INF/build.xml`, change the following line:
```xml
<property name="tomcatlib" location="/Library/Tomcat/lib"/>
```
to:
```xml
<property name="tomcatlib" location="mytomcathome/lib"/>
```

In `js/model/Constants.js`, change the following line:
```js
WEBGLOBE_SERVER: "http://localhost:8080/webglobeserver/"
```
to:
```js
WEBGLOBE_SERVER: "http://mytomcaturl:8080/webglobeserver/"
```

In `WEB-INF/src/edu/buffalo/webglobe/server/utils/Constants.java`, change the following lines:
```java
  public static final String HDFS_SERVER = "hdfs://localhost:9000";
  public static final String HDFS_BASEDIR = "/user/chandola/";
  public static final String LOCAL_TMP_DIRECTORY = "/home/ubuntu/fileserver/tmp";
  public static final String DB_USER_NAME = "root";
  public static final String DB_PASSWORD = "root";
```
to
```java
  public static final String HDFS_SERVER = "hdfs://myhdfsurl:9000";
  public static final String HDFS_BASEDIR = "myhdfsbasedir";
  public static final String LOCAL_TMP_DIRECTORY = "mylocaltmpdir";
  public static final String DB_USER_NAME = "mysqlusername";
  public static final String DB_PASSWORD = "mysqlpassword";
```
### Step 7 - Compile JAVA code
From the `WEB-INF` directory, type:
```
ant
```
### Step 8 - Run WebGlobe application
Assuming that Tomcat, HDFS, and MySQL are running, from any browser go to:
http://mytomcaturl/webglobeserver/.
