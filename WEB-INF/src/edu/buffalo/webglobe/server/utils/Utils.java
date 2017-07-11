package edu.buffalo.webglobe.server.utils;

import edu.buffalo.webglobe.server.netcdf.NetcdfColorMap;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

import static edu.buffalo.webglobe.server.utils.Constants.VALID_EXTENSIONS;

public class Utils {
    public final static Logger logger = Logger.getLogger("WEBGLOBE.LOGGER");

    public final static Configuration configuration = new Configuration();
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

    public static Vector<String[]> parseURL(String uri) throws MalformedURLException{

        try {
            Vector<String[]> allTokens = new Vector<String[]>(0);
            String delim = " \n\r,;"; //insert here all delimiters
            StringTokenizer st = new StringTokenizer(uri,delim);
            String _p = "";
            String _u = "";
            int i = 0;
            while (st.hasMoreTokens()) {
                String u = st.nextToken();

                String[] tokens = new String[3];
                if (u.indexOf("://") == -1) {
                    throw (new MalformedURLException());
                }
                String protocol = u.substring(0, u.indexOf("://"));
                String s1 = u.substring(u.indexOf("://") + 3, u.length());

                tokens[0] = protocol;
                tokens[1] = protocol + "://" + s1.substring(0, s1.indexOf('/'));
                tokens[2] = s1.substring(s1.indexOf('/'), s1.length());
                if(i == 0){
                    _p = tokens[0];
                    _u = tokens[1];
                }else{
                    if(!_p.equalsIgnoreCase(tokens[0]) && !_u.equalsIgnoreCase(tokens[1])){
                        throw (new MalformedURLException());
                    }
                }
                allTokens.add(tokens);
                i++;
            }
            return allTokens;
        }catch(Exception e) {
            return null;
        }
    }

    public static boolean isNCFile(String uri){
        return uri.lastIndexOf('.') != -1 && VALID_EXTENSIONS.contains(uri.substring(uri.lastIndexOf('.') + 1, uri.length()));
    }

    public static String cleanFileName(String fileName) {
        return fileName.replace('/','_').replace(' ','_');
    }

}
