package edu.buffalo.webglobe.server.netcdf;

/**
 * @author chandola
 * @version $Id$
 */

import edu.buffalo.webglobe.server.utils.Utils;
import ucar.ma2.Array;
import ucar.nc2.Dimension;
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


public abstract class NetcdfSource implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 6617469854915522348L;

    protected String uri;
    protected String target;
    protected String protocol;
    protected int timeLen;
    protected int latLen;
    protected int lonLen;
    protected String dataDescription = null;
    protected String datasetName = null;
    protected String datasetInfoURL = null;
    protected ArrayList<String> variables = null;
    protected ArrayList<String> units = null;
    protected CalendarDate startDate;
    protected CalendarDate endDate;
    private CalendarDate[] dates;
    private String infoURL;

    public NetcdfDataset getDataset() {
        return dataset;
    }

    protected NetcdfDataset dataset;
    protected boolean initialized;

    public NetcdfSource(String protocol, String uri, String target) throws Exception {

        this.protocol = protocol;
        this.uri = uri;
        this.target = target;
        this.initialized = false;
    }

    public void initialize() throws Exception{
        this.dataset = this.loadDataset();
        variables = new ArrayList<String>();
        units = new ArrayList<String>();

        dataDescription = NetCDFUtils.extractInfo(dataset);
        datasetName = NetCDFUtils.extractTitle(dataset);
        datasetName = NetCDFUtils.extractInfoURL(dataset);
        GridDataset gridDataset = new GridDataset(dataset);
        List grids = gridDataset.getGrids();
        if(grids.size() > 0){
            //this data set has geogrids
            for(int i = 0; i < grids.size(); i++){
                GeoGrid g = (GeoGrid) grids.get(i);
                variables.add(g.getName());
                units.add(g.getUnitsString());
            }
        }else{
            //this data set does not have geogrids, just work with the dataset
            List<Variable> vars = dataset.getVariables();
            for(int i = 0; i < vars.size(); i++){
                Variable v = vars.get(i);
                if(v.getDimensions().size() >= 3) {
                    variables.add(v.getShortName());
                    units.add(v.getUnitsString());
                    Utils.logger.info("Added variable with name " + v.getShortName());
                }
            }
        }
        List<Dimension> dims = dataset.getDimensions();
        timeLen = dims.get(0).getLength();
        latLen = dims.get(1).getLength();
        lonLen = dims.get(2).getLength();

        Array arrTime = dataset.findVariable("time").read();
        CalendarDate calDate = CalendarDateFormatter.isoStringToCalendarDate(Calendar.noleap, "2005-01-01");
        calDate = calDate.add((int) arrTime.getDouble(0), CalendarPeriod.Field.Day);
        startDate = calDate;

        calDate = CalendarDateFormatter.isoStringToCalendarDate(Calendar.noleap, "2005-01-01");
        calDate = calDate.add((int) arrTime.getDouble(0) + timeLen - 1, CalendarPeriod.Field.Day);
        endDate = calDate;

        dataset.close();
        this.initialized = true;
    }

    public abstract int getTotalTimeLength();

    protected abstract NetcdfDataset loadDataset();

    protected abstract NetcdfDataset loadDataset(int yearInd);

    public CalendarDate getStartDate() {
        return startDate;
    }

    public CalendarDate getEndDate() {
        return endDate;
    }

    public String getUri() {
        return uri;
    }

    public String getTarget() {
        return target;
    }

    public int getTimeLen() {
        return timeLen;
    }

    public ArrayList<String> getVariables(){
        return variables;
    }

    public ArrayList<String> getUnits(){
        return units;
    }

    public String getDataDescription(){ return dataDescription; }

    public String getDatasetName(){ return datasetName;}

    public int getIndexFromDate(String dateStr) {
        CalendarDate date = CalendarDateFormatter.isoStringToCalendarDate(Calendar.noleap, dateStr);
        CalendarPeriod calPer = CalendarPeriod.of(1, CalendarPeriod.Field.Day);

        return calPer.subtract(startDate, date);
    }

    public String getDateFromIndex(int i) {
        CalendarDateFormatter dateFormatter = new CalendarDateFormatter("yyyy-MM-dd");

        return dateFormatter.toString(startDate.add(i , CalendarPeriod.Field.Day));
    }

    public int getLatLen() {
        return latLen;
    }

    public int getLonLen() {
        return lonLen;
    }

    public CalendarDate[] getDates() {
        return dates;
    }

    public boolean isInitialized(){return initialized;}

    public String getInfoURL() {
        return infoURL;
    }
}

