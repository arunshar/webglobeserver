package edu.buffalo.webglobe.server.netcdf;

/**
 * @author chandola
 * @version $Id$
 */

import ucar.nc2.dataset.NetcdfDataset;

import java.io.Serializable;


public class NetcdfFile extends NetcdfSource implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 6617469854915522348L;

    public NetcdfFile(String protocol, String uri, String target) throws Exception {
        super(protocol, uri, target);
    }

    @Override
    public int getTotalTimeLength() {
        return this.timeLen;
    }

    @Override
    protected NetcdfDataset loadDataset() {
        if(this.protocol.equalsIgnoreCase("hdfs")) {
            NetcdfDataset dataset = NetcdfUtils.loadDFSNetCDFDataSet(this.uri, this.target, 3000);
            return dataset;
        }else{
            NetcdfDataset dataset = NetcdfUtils.loadHTTPNetcdfDataSet(this.uri, this.target);
            return dataset;
        }
    }

    @Override
    public NetcdfDataset loadDataset(int yearInd) {
        return loadDataset();
    }

}

