package edu.buffalo.webglobe.server.netcdf;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.buffalo.webglobe.server.utils.Utils;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
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

    private NetcdfSource netcdfSource;
	private String variableName;
    private ArrayList<CalendarDate> dates;

	public NetcdfVariable(NetcdfSource netcdfSource, String variableName){
        this.netcdfSource = netcdfSource;
        this.variableName = variableName;
        this.dates = new ArrayList<CalendarDate>();
    }
	
	public String getVariableName() {
		return variableName;
	}

    public NetcdfSource getNetcdfSource() {return netcdfSource;}

	public Array getData(int i) {
		int yearInd = i / netcdfSource.getTimeLen();
		int[] origin = {i % netcdfSource.getTimeLen(), 0, 0};
		int[] shape = {1, netcdfSource.getLatLen(), netcdfSource.getLonLen()};
		NetcdfDataset dataset = this.getNetcdfSource().loadDataset(yearInd);
		Array src;
		try {
			src = dataset.findVariable(variableName).read(origin, shape);
			dataset.close();
			return src;
		} catch (IOException e) {
			Utils.logger.severe(e.getMessage());
		} catch (InvalidRangeException e) {
            Utils.logger.severe(e.getMessage());
		}

        return null;
	}

    public List<Array> getTimeSeriesData(float x, float y){
        this.dates = new ArrayList<CalendarDate>();
        int numFiles = 1;
        if(this.netcdfSource instanceof NetcdfDirectory){
            numFiles = ((NetcdfDirectory) this.netcdfSource).getFilepaths().size();
        }

        List<Array> arrayList = new ArrayList<Array>();
        for (int i = 0; i < numFiles; i++) {
            NetcdfDataset dataset = this.getNetcdfSource().loadDataset(i);
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
                int[] shape = {this.netcdfSource.getTimeLen(), 1, 1};
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

    public ArrayList<CalendarDate> getDates() {
        return dates;
    }
}
