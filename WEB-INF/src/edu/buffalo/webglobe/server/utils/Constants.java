package edu.buffalo.webglobe.server.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {
	public static final String PUBLIC_ADDRESS = "http://199.109.195.187:8000";
	public static final String LOCAL_DIRECTORY = "/home/ubuntu/fileserver";
    public static final String DB_USER_NAME = "root";
    public static final String DB_PASSWORD = "mysql";
    public static final List<String> VALID_EXTENSIONS = Arrays.asList("nc", "h5", "netcdf", "hdf", "grib", "grib2");
    public static final List<String> VALID_DESCRIPTION_TAGS = Arrays.asList("info",  "information", "description", "about");
    public static final List<String> VALID_TITLE_TAGS = Arrays.asList("title", "name");

}
