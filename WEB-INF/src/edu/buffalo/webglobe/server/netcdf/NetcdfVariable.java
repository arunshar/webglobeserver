package edu.buffalo.webglobe.server.netcdf;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.buffalo.webglobe.server.utils.Utils;
import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarPeriod;

public class NetcdfVariable implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6617469854915522348L;

    private NetcdfDataSource netcdfDataSource;
	private String variableName;
    private ArrayList<CalendarDate> dates;
    private float min;
    private float max;
    private int latIndex;
    private int lonIndex;
    private int timeIndex;


    public NetcdfVariable(NetcdfDataSource netcdfDataSource, String variableName){
        this.netcdfDataSource = netcdfDataSource;
        this.variableName = variableName;
        this.dates = new ArrayList<CalendarDate>();
        this.min = Float.MAX_VALUE;
        this.max = Float.MIN_VALUE;
        NetcdfDataset dataset = this.netcdfDataSource.loadDataset(0);
        Variable var = dataset.findVariable(this.variableName);
        latIndex = NetcdfUtils.findLatDimensionIndex(var);
        lonIndex = NetcdfUtils.findLonDimensionIndex(var);
        timeIndex = NetcdfUtils.findTimeDimensionIndex(var);

    }
	
	public String getVariableName() {
		return variableName;
	}

    public NetcdfDataSource getNetcdfDataSource() {return netcdfDataSource;}

    public List<Array> getTimeSeriesData(float x, float y){
        this.dates = new ArrayList<CalendarDate>();
        int numFiles = this.netcdfDataSource.getNumFiles();


        List<Array> arrayList = new ArrayList<Array>();
        for (int i = 0; i < numFiles; i++) {
            NetcdfDataset dataset = this.getNetcdfDataSource().loadDataset(i);
            List<Dimension> dimensions = dataset.getDimensions();
            Dimension tDim = dimensions.get(0);
            Dimension yDim = dimensions.get(1);
            Dimension xDim = dimensions.get(2);
            CalendarDate calDate = CalendarDateFormatter.isoStringToCalendarDate(Calendar.noleap, "2005-01-01");
            try {
                Array xVals = dataset.findVariable(xDim.getShortName()).read();
                Array yVals = dataset.findVariable(yDim.getShortName()).read();
                Array tVals = dataset.findVariable(tDim.getShortName()).read();
                int xInd = -1;
                int yInd = -1;

                //find index for x
                for(int j = 0; j < xVals.getSize()-1; j++){
                    if(((xVals.getFloat(j) <= x) && (xVals.getFloat(j+1) >= x)) || ((xVals.getFloat(j+1) <= x) && (xVals.getFloat(j) >= x))){
                        xInd = j;
                        break;
                    }
                }
                //find index for y
                for(int j = 0; j < yVals.getSize()-1; j++){
                    if(((yVals.getFloat(j) <= y) && (yVals.getFloat(j+1) >= y)) || ((yVals.getFloat(j+1) <= y) && (yVals.getFloat(j) >= y))){
                        yInd = j;
                        break;
                    }
                }

                if(xInd == -1 || yInd == -1) {
                    Utils.logger.severe("Selected point is not contained in the data set.");
                    return null;
                }

                //populate dates
                for (int j = 0; j < tVals.getSize(); j++) {
                    dates.add(calDate.add((int) tVals.getDouble(j), CalendarPeriod.Field.Day));
                }
                Variable var = dataset.findVariable(variableName);
                int[] origin = {0, yInd, xInd};
                int[] shape = {this.netcdfDataSource.getTimeLen(), 1, 1};
                Array arr = var.read(origin,shape);
                arrayList.add(arr);
            }catch(IOException e){
                Utils.logger.severe("Error reading data for variable");
                return null;
            } catch (InvalidRangeException e) {
                Utils.logger.severe("Error specifying the ranges");
                return null;
            }
        }
        return arrayList;
    }

    public void writeToHDFS(BufferedWriter br) throws Exception{
        HashMap<String, List<Float>> map = this.getData();
        if(map == null){
            throw new NullPointerException();
        }
        //write out the map to the buffered writer
        for(String key: map.keySet()){
            br.write(key+":"+map.get(key)+"\n");
        }
    }

    public HashMap<String, List<Float>> getData(){
        HashMap<String, List<Float>> map = new HashMap<String, List<Float> >();
        for (int i = 0;i<this.getNetcdfDataSource().getLonLen(); i++) {
            for (int j = 0; j < this.getNetcdfDataSource().getLatLen(); j++) {
                String key = Float.toString(this.getNetcdfDataSource().getLonArr().getFloat(i)) + "," + Float.toString(this.getNetcdfDataSource().getLatArr().getFloat(j));
                List<Float> vals = new ArrayList<Float>();
                map.put(key,vals);
            }
        }
        ArrayList<Array> allVals = new ArrayList<Array>();
        for (int k = 0; k < this.netcdfDataSource.getNumFiles(); k++) {
            //open the data set
            NetcdfDataset dataset = this.netcdfDataSource.loadDataset(k);
            Variable var = dataset.findVariable(this.variableName);
            int perFileTimeLen = this.netcdfDataSource.getTimeLen() / this.netcdfDataSource.getNumFiles();
            for (int l = 0; l < perFileTimeLen; l++) {
                int[] origin = this.getOrigins(l);
                int[] shape = this.getShapes();
                Array vals;
                try {
                    vals = var.read(origin, shape);
                    allVals.add(vals);
                } catch (Exception e) {
                    Utils.logger.severe("Error reading variable data from the netcdf data source.");
                    return null;
                }
            }
        }
        Index3D idx;
        Array vals;
        int[] indices;
        float v;
        String key;
        int perFileTimeLen = this.netcdfDataSource.getTimeLen() / this.netcdfDataSource.getNumFiles();

        for (int i = 0;i<this.netcdfDataSource.getLonLen(); i++) {
            for (int j = 0; j < this.netcdfDataSource.getLatLen(); j++) {
                key = Float.toString(this.netcdfDataSource.getLonArr().getFloat(i)) + "," + Float.toString(this.netcdfDataSource.getLatArr().getFloat(j));
                for(int k = 0; k < this.netcdfDataSource.getNumFiles(); k++){
                    for (int l = 0; l < perFileTimeLen; l++) {
                        vals = allVals.get(k*this.netcdfDataSource.getStride() + l);
                        idx = new Index3D(vals.getShape());
                        indices = this.getIndices(i,j);
                        idx = (Index3D) idx.set(indices);
                        v = vals.getFloat(idx);
                        if(v < this.min) this.min = v;
                        if(v > this.max) this.max = v;
                        map.get(key).add(v);
                    }
                }
            }
        }
        return map;
    }

    public float getMin(){
        return this.min;
    }

    public float getMax(){
        return this.max;
    }


    public int[] getIndices(int iLon,int iLat){
        int[] indices = new int[3];
        indices[this.lonIndex] = iLon;
        indices[this.latIndex] = iLat;
        indices[this.timeIndex] = 0;
        return indices;
    }

    public int[] getOrigins(int l){
        int [] origins = new int[3];
        origins[this.lonIndex] = 0;
        origins[this.latIndex] = 0;
        origins[this.timeIndex] = this.getNetcdfDataSource().getStride()*l;
        return origins;
    }

    public int[] getShapes(){
        int [] shapes = new int[3];
        shapes[this.lonIndex] = this.getNetcdfDataSource().getLonLen();
        shapes[this.latIndex] = this.getNetcdfDataSource().getLatLen();
        shapes[this.timeIndex] = 1;
        return shapes;
    }
}
