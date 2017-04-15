package edu.buffalo.webglobe.server.utils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
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

	public NetcdfDir(String hdfsuri) {
		this.hdfsuri = hdfsuri;
		this.dir = hdfsuri.substring(hdfsuri.indexOf("/user"), hdfsuri.length());
		try {
			this.filepaths = NetCDFUtils.listPaths(hdfsuri, dir);
			
			// get dimension's length
			NetcdfDataset dataset = NetCDFUtils.loadDFSNetCDFDataSet(hdfsuri, filepaths.get(0), 3000);
			NetcdfFile cdfFile = dataset.getReferencedFile();
			List<Dimension> dims = cdfFile.getDimensions();
			timeLen = dims.get(0).getLength();
			latLen = dims.get(1).getLength();
			longLen = dims.get(2).getLength();
			variableName =  cdfFile.getVariables().get(3).getShortName();
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

	public String getDir() {
		return dir;
	}
	
	public int getTimeLen() {
		return timeLen;
	}

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
		
		return dateFormatter.toString(startDate.add(i , CalendarPeriod.Field.Day));
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
	
}
