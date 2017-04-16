package edu.buffalo.webglobe.server.utils;

import ucar.ma2.Array;
import ucar.ma2.MAMath;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

public class Utils {

    public static File[] createImages(NetcdfDir netcdfDir, String saveDir, String from, String to){

        File folder = new File(Constants.LOCAL_DIRECTORY + saveDir);
        folder.mkdirs();

        int startIndex = Math.max(netcdfDir.getIndexFromDate(from),0);
        int endIndex = Math.min(netcdfDir.getIndexFromDate(to),netcdfDir.getFilepaths().size()*netcdfDir.getTimeLen()-1);
        for (int i = startIndex; i <= endIndex; ++i) {
            Array src = netcdfDir.getData(i);
            float[][] data = ((float[][][]) src.copyToNDJavaArray())[0];
            MAMath.MinMax minmax;
            if (netcdfDir.getVariableName().equals("tasmax")) {
                minmax = new MAMath.MinMax(200, 350);
            } else if (netcdfDir.getVariableName().equals("ChangeDetection")) {
                minmax = new MAMath.MinMax(-1, 2);
            } else {
                minmax = MAMath.getMinMax(src);
            }

            Utils.createImage(data, (float) minmax.min, (float) minmax.max, Constants.LOCAL_DIRECTORY + saveDir + "/" + netcdfDir.getDateFromIndex(i) + ".png");
        }
        File[] listOfFiles = folder.listFiles();
        return listOfFiles;
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
