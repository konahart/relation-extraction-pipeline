/*
 * A demonstration of how a pipeline can be set up.
 * @author Laurel Hart
 */ 
package edu.washington.cs;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class PipelineDemo {
    private CoreNLPProcessor docProc;
    private Class linker;
    private Class relationAnnotator;
    private boolean VERBOSE = true;

    public PipelineDemo(String processingExtension){
        //set this up here because same class must be used for multiple parts
    	docProc = new CoreNLPProcessor(processingExtension);
        //docProc.setXMLOutput(true);
        docProc.setVerbose(VERBOSE);
    }

    public static void main(String[] args) throws Exception {
        checkArgs(args);
    
    	File input = new File(args[0]);
        File outputDir = new File(args[1]);
        //will make a subdirectory of outputDir to put AnnotatedDoc output in
        //MultiR output will be in outputDir
        File documentsDir = new File(outputDir, "/docs");
        documentsDir.mkdir();
    	File entityAliases = new File(args[2]);
    	File indexDir = new File(args[3]);

        String rawExt = ".gz";
        String procExt = ".processed.gz";
        String linkExt = ".linked.gz";
        String relExt = ".relations.gz";

        File[] inputs = Utils.findFiles(input);
        String[] filenames;
        if (inputs.length == 0){
            System.out.println("No files found.");
            System.exit(0);
        } 

        //create a list of filenames to process. 
        //each step in the pipeline will look for the output extension of the
        //previous step, and add it to the base filenames.
        //note: this will break if there is a period anywhere but before
        //the extension of the filename
        filenames = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++){
            filenames[i] = inputs[i].getName().replaceAll("\\..*$", "");
        }

        if (!input.isDirectory()){
            input = input.getParentFile();
        }

        PipelineDemo train = new PipelineDemo(procExt);

        //begin pipeline
    	train.preprocessing(filenames, input, documentsDir, rawExt);
    	train.linking(filenames, documentsDir, documentsDir, entityAliases, procExt, linkExt);
    	train.annotating(filenames, documentsDir, documentsDir, indexDir, linkExt, relExt);
        train.output(filenames, documentsDir, outputDir, relExt);
    }
    
    public static void checkArgs(String[] args) { 
    	if ( args.length <= 3 ){
            System.err.println("Required arguments: input file or directory, "
                               + "output directory, entity aliases file, "
                               + "relation index directory");
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
    }

    /*
     *	Preprocessing step
     *	Converts corpus documents to correct format (AnnotatedDoc), applies
     *	NER and sentence filtering
     */
    public void preprocessing(String[] filenameList, File inputDir, File output,
                              String inputExt) throws Exception{
        if (VERBOSE){
            System.out.println("Will write processed docs to " + output + "."); 
        }
        for (String filename : filenameList){
            File input = new File(inputDir, filename + inputExt);
            System.out.println("Searching for " + input);
            if (input.exists()) {
                if (VERBOSE){
                    System.out.println("Processing " + input);
                }
                docProc.preprocess(input, output); 
            }
        }
    }

    /*
     * Linking step
     * Links entity mentions in text to candidate entities in database
     */ 
    public void linking(String[] filenameList, File inputDir, File output, 
                        File entityAliases, String inputExt, String outputExt) 
                        throws IOException{
        if (VERBOSE){
            System.out.println("Loading Freebase relations from "
                           + entityAliases + "."); 
        }
        if (filenameList.length < 1){
            return;
        }
        MapBasedEntityLinker linker = new MapBasedEntityLinker(entityAliases, 
                                                               outputExt);
        linker.setVerbose(VERBOSE);
        for (String filename : filenameList){
            File input = new File(inputDir, filename + inputExt);
            if (input.exists()) {
                if (VERBOSE){
                    System.out.println("Linking " + input);
                }
                linker.link(input, output); 
            }
        }
    }

    /*
     * Relation annotation step
     * Finds relations between candidate entities and assumes that since the
     * entities are both present in the sentence, the sentence expresses those
     * relations.
     */
    public void annotating(String[] filenameList, File inputDir, File output,
                           File indexDirectory, String inputExt, 
                           String outputExt) throws Exception {
        if (VERBOSE){
           System.out.println("Generating relation annotations from documents.");
        }
        if (filenameList.length < 1){
            return;
        }
        LuceneRelationSearcher annotator = new LuceneRelationSearcher(
                                               indexDirectory, outputExt);
        annotator.setVerbose(VERBOSE);
        for (String filename : filenameList){
            File input = new File(inputDir, filename + inputExt);
            if (input.exists()) {
                if (VERBOSE){
                    System.out.println("Finding relations in " + input);
                }
                annotator.annotate(input, output);
            }
        }
    }

    /*
     * MultiR output step
     * Formats sentential instances found in relation annotation step so that
     * MultiR can read it.
     * Unlike previous steps, this combines all documents into one output 
     * file bundle.
     */
    public void output(String[] filenameList, File inputDir, File output, 
                       String inputExt) throws Exception {
        if (filenameList.length < 1){
            return;
        }
        if (VERBOSE){
           System.out.println("Outputting MultiR format to " + output);
        }
        HashSet<File> inputs = new HashSet<File>();
        for (String filename : filenameList){
            File input = new File(inputDir, filename + inputExt);
            if (input.exists()){
                inputs.add(input);
            }
        }
            
        MultiRCompiler multiR = new MultiRCompiler(docProc);
        multiR.setVerbose(VERBOSE);
        multiR.format(inputs.toArray(new File[inputs.size()]), output);
    }
}
