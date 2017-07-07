package edu.buffalo.webglobe.server.netcdf;

import edu.buffalo.webglobe.server.utils.Utils;
import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author chandola
 * @version $Id$
 */
public class NetcdfDataSource {
    private Vector<String []> tokens;
    private int numFiles;
    private boolean initialized;
    private List<CalendarDate> dates;
    private Array lonArr;
    private Array latArr;
    private int lonLen;
    private int latLen;
    private int timeLen;
    private String protocol;
    private String uri;
    private ArrayList<String> variables;
    private ArrayList<String> units;
    private String dataDescription;
    private String datasetName;
    private String datasetInfoURL;

    private int stride;


    public NetcdfDataSource(Vector<String []> tokens, int stride){
        this.tokens = tokens;
        this.numFiles = tokens.size();
        this.initialized = false;
        this.protocol = tokens.get(0)[0];
        if(this.protocol.equalsIgnoreCase("https")){
            //for some reason the NetCDF access routines do not work with https:// urls
            this.protocol = "http";
        }
        this.uri = tokens.get(0)[1];
        this.stride = stride;
    }

    public void initialize() throws IOException{
        List<CalendarDate> allDates = new ArrayList<CalendarDate>();
        //Assume that tokens have the same extents and geogrid variables
        for(int f = 0; f < numFiles; f++) {
            NetcdfDataset netcdfDataset = this.loadDataset(f);
            if (netcdfDataset != null) {
                if(f == 0) {
                    variables = new ArrayList<String>();
                    units = new ArrayList<String>();

                    dataDescription = NetcdfUtils.extractInfo(netcdfDataset);
                    datasetName = NetcdfUtils.extractTitle(netcdfDataset);
                    datasetInfoURL = NetcdfUtils.extractInfoURL(netcdfDataset);
                    GridDataset gridDataset = new GridDataset(netcdfDataset);
                    List grids = gridDataset.getGrids();
                    if (grids.size() > 0) {
                        GeoGrid g;
                        g = (GeoGrid) grids.get(0);
                        GridCoordSystem coordSystem = g.getCoordinateSystem();
                        latArr = coordSystem.getYHorizAxis().read();
                        lonArr = coordSystem.getXHorizAxis().read();
                        allDates = coordSystem.getCalendarDates();
                        //this data set has geogrids
                        for (Object grid : grids) {
                            g = (GeoGrid) grid;
                            variables.add(g.getName());
                            units.add(g.getUnitsString());
                        }
                    } else {
                        //check if the dataset has time, lat, and lon dimensions
                        //this data set does not have geogrids, just work with the dataset
                        Variable latV = NetcdfUtils.findLatVariable(netcdfDataset);
                        Variable lonV = NetcdfUtils.findLonVariable(netcdfDataset);
                        Variable timeV = NetcdfUtils.findTimeVariable(netcdfDataset);
                        if (latV == null || lonV == null || timeV == null) {
                            Utils.logger.severe("Did not find correct coordinate system in the dataset.");
                            return;
                        }
                        latArr = latV.read();
                        lonArr = lonV.read();

                        VariableDS varTime = (VariableDS) timeV;
                        CoordinateAxis1DTime tDim;
                        tDim = CoordinateAxis1DTime.factory(netcdfDataset, varTime, new java.util.Formatter());
                        allDates = tDim.getCalendarDates();
                        List<Variable> vars = netcdfDataset.getVariables();
                        for (Variable v : vars) {
                            if (v.getDimensions().size() >= 3) {
                                variables.add(v.getShortName());
                                units.add(v.getUnitsString());
                            }
                        }
                    }

                    latLen = (int) latArr.getSize();
                    lonLen = (int) lonArr.getSize();

                    netcdfDataset.close();
                }else{
                    GridDataset gridDataset = new GridDataset(netcdfDataset);
                    List grids = gridDataset.getGrids();
                    List<CalendarDate> _dates;
                    if (grids.size() > 0) {
                        GeoGrid g;
                        g = (GeoGrid) grids.get(0);
                        GridCoordSystem coordSystem = g.getCoordinateSystem();
                        _dates = coordSystem.getCalendarDates();
                        //this data set has geogrids
                    } else {
                        //check if the dataset has time, lat, and lon dimensions
                        //this data set does not have geogrids, just work with the dataset
                        Variable latV = NetcdfUtils.findLatVariable(netcdfDataset);
                        Variable lonV = NetcdfUtils.findLonVariable(netcdfDataset);
                        Variable timeV = NetcdfUtils.findTimeVariable(netcdfDataset);
                        if (latV == null || lonV == null || timeV == null) {
                            Utils.logger.severe("Did not find correct coordinate system in the dataset.");
                            return;
                        }

                        VariableDS varTime = (VariableDS) timeV;
                        CoordinateAxis1DTime tDim;
                        tDim = CoordinateAxis1DTime.factory(netcdfDataset, varTime, new java.util.Formatter());
                        _dates = tDim.getCalendarDates();
                    }
                    allDates.addAll(_dates);

                    netcdfDataset.close();
                }
            }
        }
        //extract dates
        this.dates = new ArrayList<CalendarDate>();
        int i = 0;
        while(i < allDates.size()){
            dates.add(allDates.get(i));
            i += this.stride;
        }

        this.timeLen = dates.size();
        this.initialized = true;
    }

    public NetcdfDataset loadDataset(int i) {
        if(this.protocol.equalsIgnoreCase("hdfs")){
            return NetcdfUtils.loadDFSNetCDFDataSet(this.uri, this.tokens.get(i)[2], 3000);
        }
        if(this.protocol.equalsIgnoreCase("http") || this.protocol.equalsIgnoreCase("https")) {
            return NetcdfUtils.loadHTTPNetcdfDataSet(this.uri, this.tokens.get(i)[2]);
        }
        if(this.protocol.equalsIgnoreCase("file")){
            return NetcdfUtils.loadLocalFileDataset(this.uri + "/" + this.tokens.get(i)[2]);
        }
        return null;
    }
    public boolean isInitialized() {
        return initialized;
    }

    public ArrayList<String> getVariables() {
        return variables;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getDataDescription() {
        return dataDescription;
    }

    public String getDatasetInfoURL() {
        return datasetInfoURL;
    }

    public int getLonLen() {
        return lonLen;
    }

    public Array getLonArr() {
        return lonArr;
    }

    public int getLatLen() {
        return latLen;
    }


    public Array getLatArr() {
        return latArr;
    }

    public int getTimeLen() {
        return timeLen;
    }

    public int getNumFiles() {
        return numFiles;
    }

    public ArrayList<String> getUnits() {
        return units;
    }

    public List<CalendarDate> getDates(){
        return this.dates;
    }

    public int getStride() {
        return stride;
    }
}
