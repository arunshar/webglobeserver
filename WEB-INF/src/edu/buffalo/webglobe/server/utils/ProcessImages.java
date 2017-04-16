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
            System.exit(1);
        }
        String hdfsAddress = args[0];
        String hdfsDir = hdfsAddress.substring(hdfsAddress.indexOf("/user"), hdfsAddress.length());
        String variableName = args[1];
        String from = args[2];
        String to = args[3];
        NetcdfDir netcdfDir = new NetcdfDir(hdfsAddress,variableName);
        String saveDir = hdfsDir + "/variable/" + netcdfDir.getVariableName()+"1";

        File[] listOfFiles = Utils.createImages(netcdfDir, saveDir, from, to);

        for(int i = 0; i < listOfFiles.length; i++){
            System.out.println(listOfFiles[i].getAbsolutePath());
        }
    }
}
