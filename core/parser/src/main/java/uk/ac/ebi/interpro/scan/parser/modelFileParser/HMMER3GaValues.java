package uk.ac.ebi.interpro.scan.parser.modelFileParser;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.io.*;
import java.nio.channels.FileLock;

/**
*
 * This class creates a map of model names to GA values, for a particular model
 * file.  If this object is being created for the first time, it parses the model
 * file to create this mapping.  This can be a little time consuming, so it then
 * saves the mapping to a Properties file with an extension MAP_FILE_EXTENSION, located
 * in the same place as the model file.  On subsequent instantiations of this object,
 * the properties file will be loaded instead to give better performance.
 * Note that this method uses java.nio.channels to ensure correct file locking, so is
 * both thread safe and multiple-process / multiple JVM safe.
 *
 * The values are stored initially as a String, comprising the sequence GA value
 * followed by white space and then the domain GA value.  They are kept in this format
 * to make placing into a properties file more simple.
 *
 * Finally this class parses this simple String to return the sequence and
 * domain GA values as double values.
 *
 * @author Phil Jones
 * @version $Id: HMMER3GaValues.java,v 1.2 2009/10/26 17:33:18 pjones Exp $
 * @since 1.0
 */

public class HMMER3GaValues {

    /**
     * Extracts the GA values in the forms "24.0 24.0" as group 1.
     */
    private static final Pattern GA_LINE_PATTERN = Pattern.compile("^GA\\s+(.+);$");
    // Strips of accession version number.
    private static final Pattern ACCESSION_PATTERN = Pattern.compile("^ACC\\s+([A-Z0-9]+)\\.?.*$");
    private static final String END_OF_RECORD = "//";

    private Properties accessionToGAProps = null;

    private static final String MAP_FILE_EXTENSION = ".accession_to_ga_map";

    private static final Map<String, Double> ACC_TO_SEQUENCE_GA = new HashMap<String, Double>();

    private static final Map<String, Double> ACC_TO_DOMAIN_GA = new HashMap<String, Double>();

    /**
     * Constructs a new Map of names to accessions based upon the model file passed in
     * as an argument.
     * @param modelFileAbsolutePath the model file to parse.
     * @throws java.io.IOException in the event of a problem reading the file or cleaning up afterwards.
     */
    public HMMER3GaValues(String modelFileAbsolutePath) throws IOException{
        // Check to see if the mapping has been dumped to a map file - if it has, load it from there for speed.
        File mapFile = createMapFileObject(modelFileAbsolutePath);
        if (mapFile.exists()){
            loadMappingFromPropertiesFile(mapFile);
        }

        else {
            // Parse the PfamModel file and store the mapping to a map file.
            parseModelFile(modelFileAbsolutePath);
            storeMappingToPropertiesFile(modelFileAbsolutePath);
        }
        // Finally stick the double values into the appropriate maps.
        for (Object accessionObject : accessionToGAProps.keySet()){
            String accession = (String) accessionObject;
            String gaString = (String) accessionToGAProps.get(accessionObject);
            String values[] = gaString.split("\\s");
            ACC_TO_SEQUENCE_GA.put(accession, new Double(values[0]));
            ACC_TO_DOMAIN_GA.put(accession, new Double(values[1]));
        }
    }

    private File createMapFileObject (String modelFileAbsolutePath){
        return new File (modelFileAbsolutePath + MAP_FILE_EXTENSION);
    }

    /**
     * Save the parsed mapping to a Properties file so they can be accessed more quickly on
     * subsequent runs.
     *
     * This method uses java.nio.channels to lock the mapping file / check for locks
     * on partially written mapping files.
     *
     * These locks only work cross-process, not cross-thread, so the method is also
     * synchronized.
     *
     * @param modelFileAbsolutePath the absolute path to the Model file.
     * @throws java.io.IOException thrown when writing / locking / closing streams.
     */
    private synchronized void storeMappingToPropertiesFile(String modelFileAbsolutePath) throws IOException{
        File mapFile = createMapFileObject(modelFileAbsolutePath);
        if (mapFile.exists()){
            return; // A different process has started creating the map file since this process loaded the mappings, so don't try to create it again.
        }
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream (mapFile);
            // Lock the file while it is being written to.
            FileLock lock = fos.getChannel().lock();
            accessionToGAProps.store(fos, "Mapping of model accessions to GA values.");
            lock.release();
        }
        finally {
            if (fos != null){
                fos.close();
            }
        }
    }

    /**
     * Load the mappings from a Properties file so they can be accessed more quickly.
     *
     * This method uses java.nio.channels to lock the mapping file / check for locks
     * on partially written mapping files.
     *
     * These locks only work cross-process, not cross-thread, so the method is also
     * synchronized.
     *
     * @param mapFile being a handle on the Properties file.
     * @throws java.io.IOException thrown when reading / locking / closing streams.
     */
    private synchronized void loadMappingFromPropertiesFile (File mapFile) throws IOException{
        FileInputStream fis = null;
        try{
            // Try reading it in - check for locks first of course.
            fis = new FileInputStream(mapFile);
            // The next line will block until the lock become available.
            // For reading getting a shared lock.
            FileLock lock = fis.getChannel().lock(0, Long.MAX_VALUE, true);
            accessionToGAProps = new Properties();
            accessionToGAProps.load(fis);
            lock.release();
        }
        finally {
            if (fis != null){
                fis.close();
            }
        }
    }

    public void parseModelFile(String modelFileAbsolutePath) throws IOException{
        BufferedReader reader = null;
        accessionToGAProps = new Properties();
        try{
            reader = new BufferedReader (new FileReader(modelFileAbsolutePath));
            String accession = null, gaValues = null;
            int lineNumber = 0;
            while (reader.ready()){
                String line = reader.readLine();
                lineNumber++;
                // Speed things up a lot... just check the first char of each line
                char firstChar = line.charAt(0);
                if (firstChar == '/' || firstChar == 'G' || firstChar == 'A'){

                    if (line.startsWith(END_OF_RECORD)){
                        if (accession == null || gaValues == null){
                            throw new IllegalStateException("The HMMER3 model file '" + modelFileAbsolutePath + "' contains a record that does not contain a accession and GA row.  PfamModel ending on line number " + lineNumber);
                        }
                        if (accessionToGAProps.containsKey(accession)){
                            throw new IllegalStateException("The HMMER3 model file '" + modelFileAbsolutePath + "' contains a duplicated model accession " + accession);
                        }
                        accessionToGAProps.put(accession, gaValues);
                        accession = null;
                        gaValues = null;
                    }
                    // the .startsWith method is faster than pattern matching,
                    // so check if the line starts with the expected word before building a Matcher.
                    else if (line.startsWith("ACC")){

                        Matcher nameMatcher = ACCESSION_PATTERN.matcher(line);
                        if (nameMatcher.matches()){
                            if (accession != null){
                                throw new IllegalStateException("The HMMER3 model file '" + modelFileAbsolutePath + "' contains a record that appears to contain more than one accession row.  PfamModel ending on line number " + lineNumber);
                            }
                            accession = nameMatcher.group(1);
                        }

                    }
                    // the .startsWith method is faster than pattern matching,
                    // so check if the line starts with the expected word before building a Matcher.
                    else if (line.startsWith("GA")){
                        Matcher accessionMatcher = GA_LINE_PATTERN.matcher(line);
                        if (accessionMatcher.matches()){
                            if (gaValues != null){
                                throw new IllegalStateException("The HMMER3 model file '" + modelFileAbsolutePath + "' contains a record that appears to contain more than one GA row.  PfamModel ending on line number " + lineNumber);
                            }
                            gaValues = accessionMatcher.group(1);
                        }
                    }
                }
            }
        }
        finally{
            if (reader != null){
                reader.close();
            }
        }
    }

    public double getSequenceGAForAccession(String modelAccession){
        return ACC_TO_SEQUENCE_GA.get(modelAccession);
    }

    public double getDomainGAForAccession(String modelAccession){
        return ACC_TO_DOMAIN_GA.get(modelAccession);
    }
}
