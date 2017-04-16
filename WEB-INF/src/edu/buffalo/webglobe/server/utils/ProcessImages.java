package edu.buffalo.webglobe.server.utils;

import java.io.File;

/**
 * @author chandola
 * @version $Id$
 */
public class ProcessImages {
    public static void main(String args[]){
        if(args.length < 3){
            System.err.println("Insufficient arguments.");
        }
        String hdfsAddress = args[0];
        String hdfsDir = hdfsAddress.substring(hdfsAddress.indexOf("/user"), hdfsAddress.length());
        String from = args[1];
        String to = args[2];
        NetcdfDir netcdfDir = new NetcdfDir(hdfsAddress);
        String saveDir = hdfsDir + "/variable/" + netcdfDir.getVariableName();

        File[] listOfFiles = Utils.createImages(netcdfDir, saveDir, from, to);

        for(int i = 0; i < listOfFiles.length; i++){
            System.out.println(listOfFiles[i].getAbsolutePath());
        }
    }
}
