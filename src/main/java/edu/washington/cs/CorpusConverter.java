/* File name: CorpusConverter.java
   Interface for converting raw corpus documents to AnnotatedDocs.
   @author Laurel Hart
*/
package edu.washington.cs;
import java.io.File;
import java.util.ArrayList;

public interface CorpusConverter {
    /*
     * Convert raw corpus file to AnnotatedDoc.
     */
    public ArrayList<AnnotatedDoc> convert(File doc) throws Exception;

    /*
     * Convert corpus file and write out.
     */
    public void convert(File input, File output) throws Exception;
}
