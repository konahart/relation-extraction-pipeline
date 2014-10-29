/* 
 * Uses a HashMap to find all the relevant entities for a given alias string
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
/*
 * @param entityAliases A HashMap where the keys are the alias strings, the values
 *                      are a list of candidate entities
 *                      e.g., "John" -> ["John Smith", "John Doe", "John Appleseed"]
 */
public class MapBasedEntityLinker implements Linker {
    private HashMap<String, HashSet<String>> entityAliases;
    private String outputExtension;
    private boolean VERBOSE = false;

    /*
     * Initialize with default output extension
     */
    public MapBasedEntityLinker(File entityAliasesFile) throws IOException {
        this(entityAliasesFile, ".linked");
    }

    /* 
     * Load a file from which to form the entity-alias HashMap.
     * @param aliasFile Expects a file with every line in the following format:
     *                  entity  alias
     *                  Can have more than one entity per alias, and more than one
     *                  alias per entity, but each pairing is on its own line.
     */
    public MapBasedEntityLinker(File entityAliasesFile, String outputExt) 
                                throws IOException {
        if (outputExt != null && !outputExt.equals("")){
            outputExtension = new String(outputExt);
        } else {
            outputExtension = "";
            System.err.println("Warning: a null or empty output extension will"
                               + " mean overwriting any input files.");
        }

        BufferedReader reader = new BufferedReader(new FileReader(
                                                           entityAliasesFile));
        entityAliases = new HashMap<String, HashSet<String>>();

        Pattern pattern = Pattern.compile("([^\\t]+)\\t([^\\t]+)");

        String line = "";
        while ((line = reader.readLine()) != null){
            Matcher entityAliasMatcher = pattern.matcher(line);
            if (entityAliasMatcher.matches()) {
                String entity = entityAliasMatcher.group(1);
                String alias = entityAliasMatcher.group(2);
                if (entityAliases.containsKey(alias)){
                        HashSet<String> entities = entityAliases.get(alias);
                        entities.add(entity);
                } else {
                        HashSet<String> entities = new HashSet<String>();
                        entities.add(entity);
                        entityAliases.put(alias, entities);
                }
            }
        }
        reader.close();
        if (VERBOSE){
            //show that entities and aliases were properly
            //associated
            for (String alias : entityAliases.keySet()){
                for (String entity : entityAliases.get(alias)){
                    System.out.println(alias + " " + entity);
                }
            }
            System.out.println(entityAliases.size() + " aliases loaded.");
        }
    }

    public void setVerbose(boolean verbose){
        VERBOSE = verbose;
    }

    public static void main(String[] args){
    	if ( args.length < 3 ){
            System.err.println("Required arguments: input file or directory, "
                               + "output directory, entity aliases file.");
            System.err.println("Too few arguments supplied. Exiting.");
            System.exit(1);
        }
    	File output = new File(args[1]);
    	if (!output.exists()){
            System.err.println("Output directory " + output + " does not "
                               + "exist.");
            System.err.println("Creating directory.") ;
            try {
                output.mkdir();
            } catch(Exception e){
                System.err.println("Unable to create directory " + output 
                                   + ".");
                System.err.println("Exiting.");
                System.exit(1);
            } 
        } else if (!output.isDirectory()){
            System.err.println("Second argument (output directory) must be "
                               + "a directory.");
            System.err.println("Exiting.");
            System.exit(1);
        }

    	File input = new File(args[0]);
    	File entityAliases = new File(args[2]);

        try {
            Linker linker = new MapBasedEntityLinker(entityAliases);
            if (input.isDirectory()){
                File[] inputs = Utils.findFiles(input);
                for (File inputFile : inputs){
                    linker.link(input, output); 
                }
            } else {
                linker.link(input, output); 
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    /* 
     * Link mentions to Freebase entities.
     * If a mention does not have any candidates, remove it from the Sentence.
     * If a Sentence does not have at least 2 mentions with at least 1 
     * candidate each, remove it from the AnnotatedDoc.
     */ 
    public void link(AnnotatedDoc doc){
        System.out.println("Linking "+doc.getID());
        Iterator<Sentence> sentIter = doc.iterator(); 
        int mentions;
        while (sentIter.hasNext()){
            Sentence sentence = sentIter.next();
            mentions = 0;
            for (String mention : sentence.getMentionInstances()){
                if (entityAliases.containsKey(mention)){
                    mentions++;
                    for (String entity : entityAliases.get(mention)){
                        System.out.println("Entity found: " + entity);
                        sentence.addCandidate(mention, entity);
                    }
                } else {
                    sentence.removeMention(mention);
                }
            }
            if (mentions < 2){
                sentIter.remove();
            } 
        }
    }

    /*
     * Call link with the default output extension.
     */
    public void link(File input, File output) throws IOException {
        link(input, output, outputExtension);
    }

    public void link(File input, File outputDir, String extension) 
                         throws IOException {
        //read in AnnotatedDocs from file
        ArrayList<AnnotatedDoc> docs = Utils.deserializeAnnotatedDocs(input);
        if (VERBOSE) {
            System.out.println(docs.size() + " docs loaded");
        }
        //add entity candidates to AnnotatedDocs
        Iterator<AnnotatedDoc> docIter = docs.iterator(); 
        AnnotatedDoc doc;
        while (docIter.hasNext()){
            doc = docIter.next(); 
            link(doc);
            //if AnnotatedDoc has no sentences left, remove it.
            if (doc.size() < 1){
                docIter.remove();
            }
        }
        //write linked AnnotatedDocs out
        String baseName = input.getName().replaceAll("\\..*$", "");
        String outName = new String(baseName + extension);
        File out = new File(outputDir, outName);
        Utils.serializeAnnotatedDocs(out, docs);
    }
}
