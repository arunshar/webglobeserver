package edu.buffalo.webglobe.server.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {
	public static final String PUBLIC_ADDRESS = "http://199.109.195.187:8000";
    //public static final String HDFS_SERVER = "hdfs://172.17.6.142:9000";
    public static final String HDFS_SERVER = "hdfs://localhost:9000";
    public static final String HDFS_BASEDIR = "/user/chandola/";
    public static final String LOCAL_TMP_DIRECTORY = "/home/ubuntu/fileserver/tmp";
    public static final String DB_USER_NAME = "root";
    public static final String DB_PASSWORD = "root";
    public static final List<String> VALID_EXTENSIONS = Arrays.asList("nc", "h5", "netcdf", "hdf", "grib", "grib2");
    public static final List<String> VALID_DESCRIPTION_TAGS = Arrays.asList("info",  "information", "description", "about");
    public static final List<String> VALID_TITLE_TAGS = Arrays.asList("title", "name");
    public static final List<String> VALID_LATITUDE_TAGS = Arrays.asList("lat", "Lat","Latitude","latitude","LATITUDE","y","Y");
    public static final List<String> VALID_LONGITUDE_TAGS = Arrays.asList("lon", "Lon","Longitude","longitude","LONGITUDE","x","X");
    public static final List<String> VALID_TIME_TAGS = Arrays.asList("time", "Time", "TIME");
    public static final String AUTHENTICATION_TYPE = "GLOBUS";//other options -- "TOMCAT_SERVER","HTTP_POST"
}
