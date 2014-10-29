/*
 * Interface for class that will add RelationAnnotations to AnnotatedDocs
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.io.File;
import java.util.ArrayList;

public interface RelationAnnotator {
    public void annotate(AnnotatedDoc doc) throws Exception;
    public void annotate(File input, File output) throws Exception;
}
