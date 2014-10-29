/*
 * Handle various file IO used throughout pipeline
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.io.File; 
import java.io.FileInputStream; 
import java.io.FilenameFilter; 
import java.io.FileOutputStream; 
import java.io.InputStream; 
import java.io.IOException; 
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

class Utils {
    /*
     * FilenameFilter where extension can be passed in rather than hardcoded
     */
    private static class FlexFilenameFilter implements FilenameFilter { 
        String ext; 

        public FlexFilenameFilter(String extension) { 
            if (extension != null){
                ext = extension; 
            } else {
                ext = "";
            }
        } 

        public boolean accept(File dir, String name) { 
            return name.endsWith(ext); 
        } 
    }

    public static FilenameFilter flexFilenameFilter(String extension){
        return new FlexFilenameFilter(extension);
    }

    /*
     * Find all files in a directory and all subdirectories.
     */
    public static File[] findFiles(File inputDir) throws IOException {
        return findFiles(inputDir, "", true);
    }

    /*
     * Find all files with extensiion in a directory and all subdirectories.
     */
    public static File[] findFiles(File inputDir, String extension) 
                                       throws IOException {
        return findFiles(inputDir, extension, true);
    }

    /*
     * Find all files with extensiion in a directory.
     * Optionally all subdirectories.
     */
    public static File[] findFiles(File inputDir, String extension, 
                                       boolean recursive) throws IOException {
        ArrayList<File> allFiles = new ArrayList<File>();
        FilenameFilter filter = flexFilenameFilter(extension);
        File[] files;
        if (inputDir.isDirectory()){
            files = inputDir.listFiles(filter);
        } else {
            files = new File[1];
            files[0] = inputDir;
        }

        if (recursive && files != null){
            for (File file : files){
                if (file.isDirectory()){
                    File[] moreFiles = findFiles(file, extension, true);
                    for (File f : moreFiles){
                        allFiles.add(f);
                    }
                } else {
                    allFiles.add(file);
                }
            }
        }
        return allFiles.toArray(new File[allFiles.size()]);
    }

    /*
     * Check whether input File is in GZIP format.
     * Return appropriate InputStream.
     */
    public static InputStream gzipCheck(File input) throws IOException{
        int magic = 0;
        try {
            RandomAccessFile raf = new RandomAccessFile(input, "r");
            magic = raf.read() & 0xff | ((raf.read() << 8) & 0xff00);
            raf.close();
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
        InputStream inputStream = new FileInputStream(input);
        if (magic == GZIPInputStream.GZIP_MAGIC) {
            inputStream = new GZIPInputStream(inputStream);
        }
        return inputStream;
    }

    /*
     * Read in AnnotatedDoc JSON.
     */
    public static AnnotatedDoc deserializeAnnotatedDoc(File input)
                               throws JsonGenerationException, 
                               JsonMappingException, IOException {
        InputStream inputStream = gzipCheck(input);
        ObjectMapper mapper = new ObjectMapper();
        try {
            AnnotatedDoc doc = mapper.readValue(inputStream, 
                                                 AnnotatedDoc.class);
            inputStream.close();
            return doc;
        } catch (JsonGenerationException e) {
            e.printStackTrace();
            System.out.println("Encountered JsonGenerationException");
        } catch (JsonMappingException e) {
            e.printStackTrace();
            System.out.println("Encountered JsonMappingException");
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Encountered IOException");
        }
        return new AnnotatedDoc();
    }

    /*
     * Read in ArrayList<AnnotatedDoc> JSON.
     */
    public static ArrayList<AnnotatedDoc> deserializeAnnotatedDocs(File input) 
                                          throws JsonGenerationException, 
                                          JsonMappingException, IOException {
        InputStream inputStream = gzipCheck(input);
        ObjectMapper mapper = new ObjectMapper();
        try {
            ArrayList<AnnotatedDoc> docs = mapper.readValue(inputStream, 
                mapper.getTypeFactory().constructCollectionType(
                ArrayList.class, AnnotatedDoc.class));
            inputStream.close();
            return docs;
        } catch (JsonGenerationException e) {
            e.printStackTrace();
            System.out.println("Encountered JsonGenerationException");
        } catch (JsonMappingException e) {
            e.printStackTrace();
            System.out.println("Encountered JsonMappingException");
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Encountered IOException");
        }
        return new ArrayList<AnnotatedDoc>();
    }

    /*
     * Write out AnnotatedDoc as GZIPed JSON file. 
     * Will be UTF-8 by default.
     */
    public static void serializeAnnotatedDoc(File output, AnnotatedDoc doc)
                       throws JsonGenerationException, JsonMappingException, 
                       IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            //create any intermediate directories that need to be created
            File parentDirs = output.getParentFile();
            if (parentDirs != null && !parentDirs.exists()){
                parentDirs.mkdirs();
            } 

            GZIPOutputStream out = new GZIPOutputStream(
                                   new FileOutputStream(output));
            mapper.defaultPrettyPrintingWriter().writeValue(out, doc);
            out.close(); 
        } catch (JsonGenerationException e) {
            e.printStackTrace();
            System.out.println("Encountered JsonGenerationException");
        } catch (JsonMappingException e) {
            e.printStackTrace();
            System.out.println("Encountered JsonMappingException");
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Encountered IOException");
        }
    }

    /*
     * Write out ArrayList<AnnotatedDoc> as GZIPed JSON file. 
     * Will be UTF-8 by default.
     */
    public static void serializeAnnotatedDocs(File output, 
                       ArrayList<AnnotatedDoc> docs) 
                       throws JsonGenerationException, JsonMappingException, 
                       IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            //create any intermediate directories that need to be created
            File parentDirs = output.getParentFile();
            if (parentDirs != null && !parentDirs.exists()){
                parentDirs.mkdirs();
            } 

            GZIPOutputStream out = new GZIPOutputStream(
                                   new FileOutputStream(output));
            mapper.defaultPrettyPrintingWriter().writeValue(out, docs);
            out.close(); 
        } catch (JsonGenerationException e) {
            e.printStackTrace();
            System.out.println("Encountered JsonGenerationException");
        } catch (JsonMappingException e) {
            e.printStackTrace();
            System.out.println("Encountered JsonMappingException");
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Encountered IOException");
        }
    }
}
