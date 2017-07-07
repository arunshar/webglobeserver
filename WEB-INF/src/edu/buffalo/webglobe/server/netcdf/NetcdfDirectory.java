package edu.buffalo.webglobe.server.netcdf;

/**
 * @author chandola
 * @version $Id$
 */

import ucar.nc2.dataset.NetcdfDataset;

import java.io.Serializable;
import java.util.ArrayList;


public class NetcdfDirectory extends NetcdfSource implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 6617469854915522348L;

    private ArrayList<String> filepaths = null;

    public NetcdfDirectory(String protocol, String uri, String target) throws Exception {
        super(protocol, uri, target);
        this.filepaths = NetcdfUtils.listPaths(uri, target);
    }

    @Override
    public int getTotalTimeLength() {
        return this.filepaths.size()*timeLen;
    }

    public ArrayList<String> getFilepaths(){return filepaths;}

    @Override
    protected NetcdfDataset loadDataset() {
        NetcdfDataset dataset = NetcdfUtils.loadDFSNetCDFDataSet(this.uri, filepaths.get(0), 3000);
        return dataset;
    }

    @Override
    public NetcdfDataset loadDataset(int yearInd) {
        NetcdfDataset dataset = NetcdfUtils.loadDFSNetCDFDataSet(this.uri, filepaths.get(yearInd), 3000);
        return dataset;
    }

}

