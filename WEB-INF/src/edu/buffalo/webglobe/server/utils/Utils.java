package edu.buffalo.webglobe.server.utils;

import edu.buffalo.webglobe.server.netcdf.NetcdfColorMap;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

public class Utils {
    public final static Logger logger = Logger.getLogger("WEBGLOBE.LOGGER");

	public static boolean createImage(float[][] data, float min, float max, String fileName) {
		int numLatitudes = data.length;
		int numLongitudes = data[0].length;
		Utils.logger.info("Size is " + numLatitudes + " and " + numLongitudes);
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

    public static String[] parseURL(String uri) throws MalformedURLException{

        try {
            String[] tokens = new String[3];
            if(uri.indexOf("://") == -1){
                throw(new MalformedURLException());
            }
            String protocol = uri.substring(0,uri.indexOf("://"));
            String s1 = uri.substring(uri.indexOf("://") + 3, uri.length());

            tokens[0] = protocol;
            tokens[1] = protocol+"://" + s1.substring(0, s1.indexOf('/'));
            tokens[2] = s1.substring(s1.indexOf('/'), s1.length());
            return tokens;
        }catch(Exception e) {
            return null;
        }
    }
}
