package utils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

public class Utils {

	public static void createSingleImage(String hdfsAddress, String variableName, String hdfsDir, String from, String to, MAMath.MinMax minmax){
		NetcdfDir netcdfDir = new NetcdfDir(hdfsAddress, variableName);
		
		String saveDir = hdfsDir + "/variable/" + netcdfDir.getVariableName();
		File folder = new File(LocalFileServer.LOCAL_DIRECTORY + saveDir);
		folder.mkdirs();
		
		int startIndex = Math.max(netcdfDir.getIndexFromDate(from),0);
		int endIndex = Math.min(netcdfDir.getIndexFromDate(to),netcdfDir.getFilepaths().size()*netcdfDir.getTimeLen()-1);
		for (int i = startIndex; i <= endIndex; ++i) {
			Array src = netcdfDir.getData(i);
			float[][] data = ((float[][][]) src.copyToNDJavaArray())[0];
			if (minmax == null){
				minmax = MAMath.getMinMax(src);
			}
			Utils.createImage(data, (float) minmax.min, (float) minmax.max, LocalFileServer.LOCAL_DIRECTORY + saveDir + "/" + netcdfDir.getDateFromIndex(i) + ".png");
		}
		File[] listofFiles = Utils.CreateImages(hdfsAddress,variableName,hdfsDir,from,to);

		Map<String, String> responseData = new HashMap<>();
		responseData.put("imagesAddress", saveDir);
		
		
		File[] listOfFiles = folder.listFiles();
		Arrays.sort(listOfFiles);
		String fName = listOfFiles[0].getName();
		responseData.put("imageMinDate", fName.substring(0, fName.lastIndexOf('.')));
		fName = listOfFiles[listOfFiles.length-1].getName();
		responseData.put("imageMaxDate", fName.substring(0, fName.lastIndexOf('.')));
	}

	public static boolean createImage(float[][] data, float min, float max, String fileName) {
		int numLatitudes = data.length;
		int numLongitudes = data[0].length;

		NetcdfColorMap ncColormap;
		try {
			ncColormap = NetcdfColorMap.createColorMap("rgb_", min, max,
					NetcdfColorMap.DEFAULT_COLORMAP_LOCATION);
			BufferedImage bufferedImage = new BufferedImage(numLongitudes, numLatitudes, BufferedImage.TYPE_INT_ARGB);
			int[] pixelArray = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();

			int cnt = 0;
			for (int i = numLatitudes - 1; i >= 0; i--) {
				for (int j = numLongitudes / 2; j < numLongitudes; j++) {
					double key = 0;
					try {
						key = data[i][j];
					} catch (ArrayIndexOutOfBoundsException e1) {
						e1.printStackTrace();
					}
					if (!Double.isNaN(key) && !Double.isInfinite(key) && key <= max && key >= min)
						pixelArray[cnt] = ncColormap.getColor((double) key).getRGB();
					cnt++;
				}
				for (int j = 0; j < numLongitudes / 2; j++) {
					double key = 0;
					try {
						key = data[i][j];
					} catch (ArrayIndexOutOfBoundsException e1) {
						e1.printStackTrace();
					}
					if (!Double.isNaN(key) && !Double.isInfinite(key) && key <= max && key >= min)
						pixelArray[cnt] = ncColormap.getColor((double) key).getRGB();
					cnt++;
				}
			}

			File f = new File(fileName);
			try {
				ImageIO.write(bufferedImage, "PNG", f);

			} catch (IOException exp) {
				exp.printStackTrace();
			}

			return true;

		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}
} 
