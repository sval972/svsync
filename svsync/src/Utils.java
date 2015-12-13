package svsync;

import java.text.DecimalFormat;

/**
 * @author Sval
 */
public class Utils {

    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024l));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024l, digitGroups)) + " " + units[digitGroups];
    }    
   
    public static String readableTransferRate(long size, long millis) {
        double rate = size / ((double)millis);
        long rateInt = Math.round(rate) * 1000l;
        return readableFileSize(rateInt);
    }    
}