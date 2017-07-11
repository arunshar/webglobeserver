package edu.buffalo.webglobe.server.utils;

import scala.collection.immutable.Stream;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

/**
 * @author chandola
 * @version $Id$
 */
public class Configuration {
    private Properties configuration;
    public Configuration(){

        this.configuration = new Properties();
        try {
            //File configFile = new File(Constants.CONFIGURATION_FILE);
            //InputStream inputStream = new FileInputStream(configFile);
            InputStream inputStream = Configuration.class.getResourceAsStream(Constants.CONFIGURATION_FILE);
            this.configuration.loadFromXML(inputStream);
            inputStream.close();
        }catch(Exception e){
            Utils.logger.severe("Error loading configuration file");
            Utils.logger.log(Level.SEVERE,e.getMessage());
        }
    }
    public String getValue(String key){
        return this.configuration.getProperty(key);
    }

}
