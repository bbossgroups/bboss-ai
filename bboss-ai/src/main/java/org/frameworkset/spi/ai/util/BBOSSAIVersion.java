package org.frameworkset.spi.ai.util;

import org.frameworkset.spi.BBOSSVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BBOSSAIVersion {
	public static final String description="6.5.6";
    public static final short version=656;
    public static final String releaseDate = "20260722";
    private static Logger logger = LoggerFactory.getLogger(BBOSSVersion.class);
    static {
        logger.info(getVersionDescription());
    }

    /**
     * Prints the value of the description and fields to System.out.
     * @param args
     */
    public static void main(String[] args) {
        logger.info(getVersionDescription()); 
    }

    /**
     * Returns the catenation of the description and cvs fields.
     * @return String with description
     */
    public static String getVersionDescription() {
        return "bboss ai framework Version: " + description + ",Release Date: " + releaseDate ;
    }

 
    /**
     * Compares the specified version number against the current version number.
     * @param v short
     * @return Result of == operator.
     */
    public static boolean compareTo(int v) {
        return version == v;
    }

    public static String getVersion635(){
        return description+"_"+releaseDate;
    }

    public static String getVersion(){
        return description;
    }
}
