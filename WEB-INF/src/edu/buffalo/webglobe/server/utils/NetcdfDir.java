package edu.buffalo.webglobe.server.utils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarPeriod;

public class NetcdfDir implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6617469854915522348L;
	
	private String hdfsuri;
	private String dir;
	private String variableName;
	private ArrayList<String> filepaths = null; 
	private int timeLen;
	private int latLen;
	private int longLen;
	private CalendarDate startDate;
	private CalendarDate endDate;
    private List<CalendarDate> dates;
    private static final Logger logger = Logger.getLogger("WEBGLOBE.LOGGER");

	public NetcdfDir(String hdfsuri, String varName){
        String [] tokens = Utils.parseHDFSURL(hdfsuri);
        if(tokens == null){
            return;
        }
        this.hdfsuri = tokens[0];
        this.dir = tokens[1];
		try {
			this.filepaths = NetCDFUtils.listPaths(hdfsuri, dir);

			// get dimension's length
			NetcdfDataset dataset = NetCDFUtils.loadDFSNetCDFDataSet(hdfsuri, filepaths.get(0), 3000);
			NetcdfFile cdfFile = dataset.getReferencedFile();
			List<Dimension> dims = cdfFile.getDimensions();
			timeLen = dims.get(0).getLength();
			latLen = dims.get(1).getLength();
			longLen = dims.get(2).getLength();
            variableName =  varName;

			Array arrTime = cdfFile.findVariable("time").read();
			
			CalendarDate calDate = CalendarDateFormatter.isoStringToCalendarDate(Calendar.noleap, "2005-01-01");
			calDate = calDate.add((int) arrTime.getDouble(0), CalendarPeriod.Field.Day);
			startDate = calDate;
			
			calDate = CalendarDateFormatter.isoStringToCalendarDate(Calendar.noleap, "2005-01-01");
			calDate = calDate.add((int) arrTime.getDouble(0) + filepaths.size()*timeLen - 1, CalendarPeriod.Field.Day);			
			endDate = calDate;		
			
			dataset.close();			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
    }
	
	public String getVariableName() {
		return variableName;
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

    public List<CalendarDate> getDates() {return dates;}

	public ArrayList<String> getFilepaths() {
		return filepaths;
	}

	public int getIndexFromDate(String dateStr) {
		CalendarDate date = CalendarDateFormatter.isoStringToCalendarDate(Calendar.noleap, dateStr);
		CalendarPeriod calPer = CalendarPeriod.of(1, CalendarPeriod.Field.Day);
		
		return calPer.subtract(startDate, date);
	}
	
	public String getDateFromIndex(int i) {
		CalendarDateFormatter dateFormatter = new CalendarDateFormatter("yyyy-MM-dd");

		return dateFormatter.toString(startDate.add(i, CalendarPeriod.Field.Day));
	}

	public Array getData(int i) {
		int yearInd = i / timeLen;
		int[] origin = {i % timeLen, 0, 0};
		int[] shape = {1, latLen, longLen};
		
		NetcdfDataset dataset = NetCDFUtils.loadDFSNetCDFDataSet(hdfsuri, filepaths.get(yearInd), 10000);
		NetcdfFile cdfFile = dataset.getReferencedFile();		
		Array src;
		try {
			src = cdfFile.findVariable(variableName).read(origin, shape);
			dataset.close();
			return src;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return null;
	}

    public List<Array> getTimeSeriesData(float x, float y){
        dates = new ArrayList<CalendarDate>();

        List<Array> arrayList = new ArrayList<Array>();
        for (int i = 0; i < filepaths.size(); i++) {
            NetcdfDataset dataset = NetCDFUtils.loadDFSNetCDFDataSet(hdfsuri, filepaths.get(i), 10000);
            NetcdfFile cdfFile = dataset.getReferencedFile();
            List<Dimension> dimensions = cdfFile.getDimensions();
            Dimension tDim = dimensions.get(0);
            Dimension yDim = dimensions.get(1);
            Dimension xDim = dimensions.get(2);
            CalendarDate calDate = CalendarDateFormatter.isoStringToCalendarDate(Calendar.noleap, "2005-01-01");
            try {
                Array xVals = cdfFile.findVariable(xDim.getShortName()).read();
                Array yVals = cdfFile.findVariable(yDim.getShortName()).read();
                Array tVals = cdfFile.findVariable(tDim.getShortName()).read();
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
                    logger.severe("Selected point is not contained in the data set.");
                    return null;
                }

                //populate dates
                for(int j = 0; j < tVals.getSize();j++){
                    dates.add(calDate.add((int) tVals.getDouble(j), CalendarPeriod.Field.Day));
                }
                Variable var = cdfFile.findVariable(variableName);
                int[] origin = {0, yInd, xInd};
                int[] shape = {timeLen, 1, 1};
                Array arr = var.read(origin,shape);
                arrayList.add(arr);
            }catch(IOException e){
                logger.severe("Error reading data for variable");
                return null;
            } catch (InvalidRangeException e) {
                logger.severe("Error specifying the ranges");
                return null;
            }
        }
        return arrayList;
    }
	
}
