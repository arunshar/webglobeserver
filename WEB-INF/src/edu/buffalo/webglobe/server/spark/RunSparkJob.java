package edu.buffalo.webglobe.server.spark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.apache.spark.launcher.SparkAppHandle;
import org.apache.spark.launcher.SparkLauncher;

public class RunSparkJob {

    private static final Logger logger = Logger.getLogger("WEBGLOBE.LOGGER");
	public static String[] createSparkCluster(String scriptPath, int noNodes,
			String typeNode) {
		try {
			String command = "ssh -i  /home/ubuntu/privateKeys/dtran2.private -o StrictHostKeychecking=no centos@172.17.49.178";
			//String command = "ssh -i /home/dtran/Data/work/Aristotle/privatekeys/aristotlecloud/dtran2.pem -o StrictHostKeychecking=no centos@199.109.195.163";
			command = command + " " + scriptPath + " -n " + noNodes
					+ " -k dtran2 -g linux -t " + typeNode;
			logger.info(command);
			String[] outputs = new String[3];
			Process p = Runtime.getRuntime().exec(command);
			InputStream in = p.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(in), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				System.out.println(line);
				String[] tokens = line.split(" ");
				if (tokens[0].equals("RESERVATION"))
					outputs[0] = tokens[1]; // reservationid
				else if (tokens[0].equals("MASTER")) {
					outputs[1] = tokens[1]; // publicAddress
					outputs[2] = tokens[2]; // privateAddress
				}
			}
			in.close();
			bufferedReader.close();
			p.waitFor();
			logger.info("Create exit code: " + p.exitValue());

			return outputs;
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
		return null;
	}

	public static void runSparkJob(String jarFilePath, String className, String[] args, String sparkHome,
			String masterIP, String driverMem, String executorMem, String[] additionalJars) {
		try {
			SparkLauncher launcher = new SparkLauncher()
					.setAppResource(jarFilePath).setMainClass(className)
					.setMaster("spark://" + masterIP + ":7077")
					.setSparkHome(sparkHome)
					.setConf(SparkLauncher.DRIVER_MEMORY, driverMem)
					.setConf(SparkLauncher.EXECUTOR_MEMORY, executorMem)
					.addAppArgs(args);
			for (String jar : additionalJars) {
				launcher = launcher.addJar(jar);
			}
			
			SparkAppHandle handle = launcher.startApplication(new SparkAppHandle.Listener() {
						@Override
						public void infoChanged(SparkAppHandle arg0) {
							// arg0.notify();
						}

						@Override
						public void stateChanged(SparkAppHandle arg0) {
							arg0.notify();
						}

					});

			while (!handle.getState().isFinal()) {
				synchronized (handle) {
					handle.wait();
					logger.info(handle.getState().toString());
				}
			}

		} catch (IOException e) {
			logger.severe(e.getMessage());
		} catch (InterruptedException e) {

			logger.severe(e.getMessage());
		}
	}

	public static void terminateCluster(String terminateScriptPath,
			String reservationid) {
		try {
			logger.info("TERMINATE SPARK CLUSTER ...... ");

			String command = "ssh -i  /home/ubuntu/privateKeys/dtran2.private -o StrictHostKeychecking=no centos@172.17.49.178";
			//String command = "ssh -i /home/dtran/Data/work/Aristotle/privatekeys/aristotlecloud/dtran2.pem -o StrictHostKeychecking=no centos@199.109.195.163";
			command = command + " " + terminateScriptPath + " " + reservationid;
			Process p = Runtime.getRuntime().exec(command);
			InputStream in = p.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(in), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				System.out.println(line);
			}
			in.close();
			bufferedReader.close();
			p.waitFor();
			logger.info("Terminate exit code: " + p.exitValue());

		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
	}
}
