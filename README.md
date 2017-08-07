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
mysql -u mysqluser -pmysqlpassword < setup/webglobeserver.dump.sql
```
### Step 4 - Install ant
Follow directions at http://ant.apache.org/
### Step 5 - Install Hadoop Distributed File System (HDFS)
Follow directions at https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/SingleCluster.html for single node or https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/ClusterSetup.html multi-node installation.

We will assume that `myhdfsurl` is the base HDFS url and `myhdfsbasedir` is a writable directory within the HDFS where the data will be stored. Additionally, we will assume that `mylocaltmpdir` is a writable directory (e.g., `/tmp`) on the local file system of the machine running the Tomcat web server.
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

In `WEB-INF/src/config/configuration.xml`, change the following lines:
```xml
  <entry key="HDFS_SERVER">hdfs://localhost:9000</entry>
  <entry key="HDFS_BASEDIR">/user/chandola/</entry>
  <entry key="LOCAL_TMP_DIRECTORY">/home/ubuntu/fileserver/tmp</entry>
  <entry key="DB_USER_NAME">root</entry>
  <entry key="DB_PASSWORD">root</entry>
```
to
```xml
  <entry key="HDFS_SERVER">hdfs://myhdfsurl</entry>
  <entry key="HDFS_BASEDIR">myhdfsbasedir</entry>
  <entry key="LOCAL_TMP_DIRECTORY">mylocaltmpdir</entry>
  <entry key="DB_USER_NAME">mysqluser</entry>
  <entry key="DB_PASSWORD">mysqlpassword</entry>
```
### Step 7 - Add a valid Google API Key
In `js/keys/keys.js`, change
```
var keys = {
	GOOGLE_API_KEY: ''
}
```
to
```
var keys = {
	GOOGLE_API_KEY: validkey
}
```
where validkey is a Google API Key. For more information on obtaining a Google API Key, check out https://developers.google.com/maps/documentation/geocoding/get-api-key

### Step 8 - Change username in path information
In 'WEB-INF/src/edu/buffalo/webglobe/server/spark/Correlation.java' change path value of variable name 'writer' by editing name of respective system. For example :
```
PrintWriter writer = new PrintWriter("/Users/'your-username'/Desktop/"+analysisoutputname+".txt", "UTF-8");
```
### Step 9 - Compile JAVA code
From the `WEB-INF` directory, type:
```
ant
```
### Step 10 - Run WebGlobe application
Assuming that Tomcat, HDFS, and MySQL are running, from any browser go to:
http://mytomcaturl/webglobeserver/.
