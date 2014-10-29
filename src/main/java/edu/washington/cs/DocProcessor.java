/* File name: DocProcessor.java   
 *  Interface for class that will implement Natural Language Processing
 *  on Gigaword docs.
 *  @author Laurel Hart
 */
package edu.washington.cs;

import java.io.File;
import java.util.ArrayList;

public interface DocProcessor {
    public void preprocess(File input, File output) throws Exception;
    public ArrayList<MultiRInstance> process(File doc) throws Exception;
}
