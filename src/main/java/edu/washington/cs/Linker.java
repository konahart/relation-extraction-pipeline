/*
 * Interface for class that will take mentions and link them to an entity
 * Simple implementation: mention -> entity (simple string matching)
 * Potential implementation: attempt Named Entity Linking, coref
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.io.File;
import java.util.ArrayList;
import java.io.IOException;

public interface Linker {
    public void link(AnnotatedDoc doc) throws IOException;
    public void link(File docs, File output) throws IOException;
}
