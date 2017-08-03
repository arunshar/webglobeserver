package edu.buffalo.webglobe.server.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {
  //all configuration elements here
  public static final List<String> VALID_EXTENSIONS = Arrays.asList("nc", "h5", "netcdf", "hdf", "grib", "grib2");
  public static final List<String> VALID_DESCRIPTION_TAGS = Arrays.asList("info",  "information", "description", "about");
  public static final List<String> VALID_TITLE_TAGS = Arrays.asList("title", "name");
  public static final List<String> VALID_LATITUDE_TAGS = Arrays.asList("lat", "Lat","Latitude","latitude","LATITUDE","y","Y");
  public static final List<String> VALID_LONGITUDE_TAGS = Arrays.asList("lon", "Lon","Longitude","longitude","LONGITUDE","x","X");
  public static final List<String> VALID_TIME_TAGS = Arrays.asList("time", "Time", "TIME");
  public static final String CONFIGURATION_FILE = "/config/configuration.xml";
}
