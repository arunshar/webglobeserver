package edu.buffalo.webglobe.server.utils;

/**
 * @author chandola
 * @version $Id$
 */

import ucar.ma2.Array;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarPeriod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class NetcdfDirNoVar implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 6617469854915522348L;

    private String hdfsuri;
    private String dir;
    private ArrayList<String> filepaths = null;
    private int timeLen;
    private int latLen;
    private int longLen;
    private ArrayList<String> variables = null;
    private ArrayList<String> units = null;
    private ArrayList<String> descriptions = null;
    private CalendarDate startDate;
    private CalendarDate endDate;
    private Logger logger;

    public NetcdfDirNoVar(String hdfsuri) throws Exception {
        this.logger = Logger.getLogger("WEBGLOBE.LOGGER");
        String [] tokens = Utils.parseHDFSURL(hdfsuri);
        if(tokens == null){
            return;
        }

        this.hdfsuri = tokens[0];
        this.dir = tokens[1];

        this.filepaths = NetCDFUtils.listPaths(hdfsuri, dir);

        // get dimension's length

        NetcdfDataset dataset = NetCDFUtils.loadDFSNetCDFDataSet(this.hdfsuri, filepaths.get(0), 3000);
        NetcdfFile cdfFile = dataset.getReferencedFile();
        variables = new ArrayList<String>();
        units = new ArrayList<String>();
        descriptions = new ArrayList<String>();
        GridDataset gridDataset = new GridDataset(dataset);
        List grids = gridDataset.getGrids();
        if(grids.size() > 0){
            //this data set has geogrids
            for(int i = 0; i < grids.size(); i++){
                GeoGrid g = (GeoGrid) grids.get(i);
                variables.add(g.getName());
                units.add(g.getUnitsString());
                descriptions.add(g.getDescription());
            }
        }else{
            //this data set does not have geogrids, just work with the dataset
            List<Variable> vars = dataset.getVariables();
            for(int i = 0; i < vars.size(); i++){
                Variable v = vars.get(i);
                variables.add(v.getShortName());
                units.add(v.getUnitsString());
                descriptions.add(v.getDescription());
                logger.severe("Added variable with name "+v.getShortName());
            }
        }



        List<Dimension> dims = cdfFile.getDimensions();
        timeLen = dims.get(0).getLength();
        latLen = dims.get(1).getLength();
        longLen = dims.get(2).getLength();
        Array arrTime = cdfFile.findVariable("time").read();

        CalendarDate calDate = CalendarDateFormatter.isoStringToCalendarDate(Calendar.noleap, "2005-01-01");
        calDate = calDate.add((int) arrTime.getDouble(0), CalendarPeriod.Field.Day);
        startDate = calDate;

        calDate = CalendarDateFormatter.isoStringToCalendarDate(Calendar.noleap, "2005-01-01");
        calDate = calDate.add((int) arrTime.getDouble(0) + filepaths.size()*timeLen - 1, CalendarPeriod.Field.Day);
        endDate = calDate;

        dataset.close();
    }


    public CalendarDate getStartDate() {
        return startDate;
    }

    public CalendarDate getEndDate() {
        return endDate;
    }

    public String getHdfsuri() {
        return hdfsuri;
    }

    public String getDir() {
        return dir;
    }

    public int getTimeLen() {
        return timeLen;
    }

    public ArrayList<String> getVariables(){
        return variables;
    }

    public ArrayList<String> getFilepaths() {
        return filepaths;
    }

    public ArrayList<String> getUnits(){
        return units;
    }

    public ArrayList<String> getDescriptions(){
        return descriptions;
    }

    public int getIndexFromDate(String dateStr) {
        CalendarDate date = CalendarDateFormatter.isoStringToCalendarDate(Calendar.noleap, dateStr);
        CalendarPeriod calPer = CalendarPeriod.of(1, CalendarPeriod.Field.Day);

        return calPer.subtract(startDate, date);
    }

    public String getDateFromIndex(int i) {
        CalendarDateFormatter dateFormatter = new CalendarDateFormatter("yyyy-MM-dd");

        return dateFormatter.toString(startDate.add(i , CalendarPeriod.Field.Day));
    }
}

