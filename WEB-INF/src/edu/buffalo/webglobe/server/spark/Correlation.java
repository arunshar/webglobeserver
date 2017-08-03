package edu.buffalo.webglobe.server.spark;

import edu.buffalo.webglobe.server.db.DBUtils;
import edu.buffalo.webglobe.server.utils.Utils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Progressable;

import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

public class Correlation {

    private int id;
    private String fieldName;
    private String fromDate;
    private String toDate;
    private List<Date> dates;
    private float lonMin;
    private float lonMax;
    private float lonDelta;
    private int lonNum;
    private float latMin;
    private float latMax;
    private float latDelta;
    private int latNum;
    private Date timeMin;
    private Date timeMax;
    private int timeNum;
    private String units;
    private float minValue;
    private float maxValue;
    private int startTimeIndex;
    private int endTimeIndex;
    private int boundedTimeNum;
    private String hdfsPath;
//    private int year;

    private boolean initialized;
    private float[] bounds;
    private boolean boundTimeNum;

    private boolean flipLat;
    private boolean flipLon;
    private boolean crossesDateLine;

    public Correlation(int id, String fieldName, String toDate, String fromDate){
        this.id = id;
        this.fieldName = fieldName;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.initialized = false;
//        this.year = year;
        this.initialize();

    }
    private void initialize() {
        try {
        Connection conn = DBUtils.getConnection();
        Statement stmt = conn.createStatement();
        String cmd = "SELECT D.lon_min, D.lon_max, D.lon_num, D.lat_min, D.lat_max, D.lat_num, "+
                    "D.time_min, D.time_max, D.time_num, V.units, V.min_value, V.max_value, V.hdfs_path "+
                    "FROM netcdf_datasets as D, netcdf_dataset_fields as V where D.id = "+this.id+" AND V.dataset_id = D.id AND V.field_name = \""+this.fieldName+"\"";

        ResultSet rset = DBUtils.executeQuery(conn,stmt,cmd);
        if(rset.next()){
            this.lonMin = rset.getFloat(1);
            this.lonMax = rset.getFloat(2);
            this.lonNum = rset.getInt(3);
            this.latMin = rset.getFloat(4);
            this.latMax = rset.getFloat(5);
            this.latNum = rset.getInt(6);
            this.timeMin = rset.getDate(7);
            this.timeMax = rset.getDate(8);
            this.timeNum = rset.getInt(9);
            this.units = rset.getString(10);
            this.minValue = rset.getFloat(11);
            this.maxValue = rset.getFloat(12);
            this.hdfsPath = rset.getString(13);

            //get bounding box
            this.bounds = new float[4];
            bounds[0] = Math.min(latMin,latMax);
            bounds[1] = Math.max(latMin,latMax);
            bounds[2] = Math.min(lonMin,lonMax);
            bounds[3] = Math.max(lonMin,lonMax);

            //get deltas
            this.latDelta = (bounds[1]-bounds[0])/latNum;
            this.lonDelta = (bounds[3]-bounds[2])/lonNum;

            long numMillis = 1000*60*60*24;
            long numDays = (long) Math.ceil((this.timeMax.getTime() - this.timeMin.getTime())/numMillis);
            long incr = (long) Math.ceil(numDays/this.timeNum);
            this.dates = new ArrayList<Date>();
            for(int i = 0; i < this.timeNum; i++){
                Date n = new Date(this.timeMin.getTime() + i*incr*numMillis);
                this.dates.add(n);
            }

            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Date _d;
                _d = format.parse(this.toDate);
                this.startTimeIndex = (int) Math.ceil((_d.getTime() - this.timeMin.getTime())/(incr*numMillis));
                _d = format.parse(this.fromDate);
                this.endTimeIndex = (int) Math.ceil((_d.getTime() - this.timeMin.getTime())/(incr*numMillis));
            } catch (ParseException e){
                this.startTimeIndex = 0;
                this.endTimeIndex = this.timeNum - 1;
            }
            if(this.startTimeIndex >= this.endTimeIndex){
                this.startTimeIndex = 0;
                this.endTimeIndex = this.timeNum - 1;
            }
            this.boundedTimeNum = this.endTimeIndex - this.startTimeIndex;
            this.flipLat = this.isLatFlipped();
            this.flipLon = this.isLonFlipped();
            this.crossesDateLine = this.isCrossingDateLine();
            this.initialized = true;
        }
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            Utils.logger.severe(e.getMessage());
            this.initialized = false;
        }

    }

    public HashMap<String, ArrayList<String>> readData(){
        try {
            Configuration conf = new Configuration();
            FileSystem hdfs = FileSystem.get( new URI( Utils.configuration.getValue("HDFS_SERVER") ), conf );

            Path file = new Path(this.hdfsPath);
            HashMap<String, ArrayList<Float>> returnmap = new HashMap<>();
            BufferedReader br = new BufferedReader(new InputStreamReader(hdfs.open(file)));
            String line;
            String[] data = new String[this.latNum*this.lonNum*this.boundedTimeNum];
            while (true){
                line = br.readLine();
                if(line == null)
                    break;
                String[] tokens = line.split(":");
                String[] latlon = tokens[0].split(",");
                float lon = Float.parseFloat(latlon[0]);
                float lat = Float.parseFloat(latlon[1]);
                int st = (this.getLatIndex(lat,this.flipLat)*this.lonNum*this.boundedTimeNum) + (this.getLonIndex(lon,this.flipLon)*this.boundedTimeNum);
                String st1 = String.valueOf(st);
                ArrayList<Float> location = new ArrayList<Float>();
                String[] vals = tokens[1].substring(1,tokens[1].length()-1).split(",");
                for(int i = this.startTimeIndex; i < this.endTimeIndex; i++){
                    data[st+i-this.startTimeIndex] = vals[i];
                    location.add(Float.valueOf(vals[i]));
                }
                returnmap.put(st1,location);
            }

            for (Map.Entry<String, ArrayList<Float>> entry : returnmap.entrySet()) {
                System.out.println(entry.getKey()+" : "+entry.getValue());
//            writer.println(entry.getKey()+" : "+entry.getValue());
            }

            String location = "4731804";
            ArrayList<Float> timeseries = returnmap.get(location);
            final double[] locdata = new double[timeseries.size()];
            for (int i =0; i < locdata.length; i++){
                locdata[i] = timeseries.get(i);
            }


            HashMap<String,ArrayList<String>> corrmap = new HashMap<>();
            for (Map.Entry<String, ArrayList<Float>> entry : returnmap.entrySet()){
                int year = 1950;
                int index = (year-1948)*12;
                final double[] worlddata = new double[12];
                int j = 0;
                for (int i = index; i < index+12; i++){
                    worlddata[j] = entry.getValue().get(i);
                    j = j + 1;
                }
                System.out.println(worlddata);
                System.out.println("hi Hi Hi");
                ArrayList<String> corrdata = new ArrayList<String>();
                int i = 0;
                int count = 0;
                while (i<locdata.length){
                    double[] newArray = Arrays.copyOfRange(locdata, i, i+12);
                    double corr = new PearsonsCorrelation().correlation(newArray,worlddata);
                    corrdata.add(String.valueOf(corr));
                    i = i + 12;
                }
                corrmap.put(entry.getKey(),corrdata);
            }
            System.out.println(corrmap);

//            Configuration configuration = new Configuration();
//            FileSystem hdfs_csv = FileSystem.get( new URI( Utils.configuration.getValue("HDFS_SERVER") ), configuration );
//            String hdfsFileName = Utils.configuration.getValue("HDFS_BASEDIR")+"/"+"corrmap"+"/"+"correlationmap1.csv";
//            Path csv_file = new Path(hdfsFileName);
//
//            if(hdfs_csv.exists(csv_file)) {hdfs.delete( file, true );}
//            OutputStream os = hdfs_csv.create(csv_file,
//                    new Progressable() {
//                        @Override
//                        public void progress() {
//                            Utils.logger.info(".");
//                        }
//                    });
//            BufferedWriter brtocsv = new BufferedWriter((new OutputStreamWriter(os,"UTF-8")));
//            try {
//                for(String key: corrmap.keySet()){
//                    brtocsv.write(key+":"+corrmap.get(key)+"\n");
//                }
//                brtocsv.close();
//                hdfs_csv.close();
//            }catch(Exception e){
//                br.close();
//                hdfs.close();
//                Utils.logger.severe("Error in writing out data to HDFS");
//            }

//            ArrayList<String> strData = new ArrayList<String>();
//            for(String s: data){
//                strData.add(s);
//            }

            return corrmap;
        }catch(Exception e){
            Utils.logger.severe("Error reading data from HDFS");
            Utils.logger.log(Level.SEVERE,e.getMessage());
            return null;
        }
    }

    public int getLonIndex(double lon, boolean flip) {
        int i = (int) Math.floor((lon - this.bounds[2])/this.lonDelta);
        if(flip) {
            i = this.lonNum - 1 - i;
        }
        if(this.crossesDateLine){
            i = (i + this.lonNum/2)%this.lonNum;
        }
        if(i >= this.lonNum)
            return this.lonNum - 1;
        if(i < 0)
            return 0;
        return i;
    }

    public int getLatIndex(double lat, boolean flip) {
        int i = (int) Math.floor((lat - this.bounds[0])/this.latDelta);
        if(flip){
            i = this.latNum - 1 - i;
        }
        if(i >= this.latNum)
            return this.latNum - 1;
        if(i < 0)
            return 0;
        return i;
    }

    public int getStartTimeIndex() {
        return startTimeIndex;
    }

    public int getEndTimeIndex(){
        return endTimeIndex;
    }

    public List<Date> getDates() {
        return dates;
    }

    public float[] getBounds() {
        return bounds;
    }

    public int getLatNum() {
        return latNum;
    }

    public float getLatDelta() {
        return latDelta;
    }

    public int getTimeNum() {
        return timeNum;
    }

    public int getBoundedTimeNum() {
        return boundedTimeNum;
    }

    public int getLonNum() {
        return lonNum;
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public boolean isLatFlipped(){
        return true;
    }

    public boolean isLonFlipped(){
        return false;
    }

    public boolean isCrossingDateLine(){
        return true;
    }
}
