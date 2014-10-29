/*
 * Output sentential (training) instances in the format needed for use by MultiR
 * Needs to be given DocProcessor that was used in earlier stage of pipeline
 * to do NER
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

/*
 * @param negativeProportion    Used to determine the proportion of
 *                              negative to posiive examples
 *                              Default is 4:1
 */
public class MultiRCompiler {
    private int negativeProportion;
    private DocProcessor processor;
    private OutputStream meta;
    private OutputStream text;
    private OutputStream sentOffset;
    private OutputStream tokenOffset;
    private OutputStream tokenNER;
    private OutputStream sentDependency;
    private OutputStream tokenPOS;
    private OutputStream instance;
    private boolean VERBOSE = false;

    /*
     * Initialize with all default properties. 
     * Using default DocProcessor not advised.
     */
    public MultiRCompiler(){
    	this(new CoreNLPProcessor(), 4);
    }

    public MultiRCompiler(DocProcessor docProc) {
        this(docProc, 4);
    }

    public MultiRCompiler(DocProcessor docProc, int negProp) {
        negativeProportion = negProp;
        processor = docProc;
    }

    public void setVerbose(boolean verbose){
        VERBOSE = verbose;
    }

    public static void main(String[] args) throws Exception {
    	if ( args.length <= 1 ){
            System.err.println("Required arguments: input file, output directory");
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
                System.err.println("Unable to create directory " + output +".");
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

        MultiRCompiler multiR = new MultiRCompiler();
        multiR.format(input, output); 
    }

    /*
     * Set up all output files
     */
    private void openFiles(File output) throws IOException {
        try { 
            meta = new FileOutputStream(new File(output, "sentences.meta"));
            text = new FileOutputStream(new File(output, 
                   "SENTTEXTINFORMATION")); 
            sentOffset = new FileOutputStream(new File(output, 
                         "SENTOFFSETINFORMATION"));
            tokenOffset = new FileOutputStream(new File(output, 
                          "TOKENOFFSETINFORMATION"));
            tokenNER = new FileOutputStream(new File(output, 
                       "TOKENNERINFORMATION"));
            sentDependency = new FileOutputStream(new File(output, 
                             "SENTDEPENDENCYINFORMATION"));
            tokenPOS = new FileOutputStream(new File(output, 
                       "TOKENPOSINFORMATION"));
            instance = new FileOutputStream(new File(output, "training.inst"));
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Encountered IOException");
        }
    }

    /*
     * Given a set of files, all sentential instances from those files will be
     * written to one output file, in MultiR format.
     */
    public void format(File[] inputs, File outputDir) throws Exception {
        //set up output files
        openFiles(outputDir);

        for (File input : inputs){
            System.out.println("Adding " + input);
            output(input, outputDir);
        }

        closeFiles();
    }

    /*
     * Write out sentential instances from files in MultiR format.
     */
    public void format(File input, File outputDir) throws Exception {
        //set up output files
        openFiles(outputDir);

        output(input, outputDir);

        closeFiles();
    }

    /*
     * Use DocProcessor processor to do any processing needed on sentential
     * instances.
     */
    private void output(File input, File output) throws Exception {
        ArrayList<MultiRInstance> trainInsts = processor.process(input);

        for (MultiRInstance trainInst : trainInsts){
            ArrayList<String> relations = trainInst.getPositives();
            ArrayList<String> negatives = trainInst.getNegatives();
            int positiveCount = relations.size();
            int negativeCount = negatives.size();
            int negMax = 0;

            if (VERBOSE) {
                System.out.println(positiveCount + " positive examples");
                System.out.println(negativeCount + " negative examples");
            }

            //if there are no positive examples, don't bother with the 
            //next steps
            if (positiveCount == 0){
                System.out.println("0 negative examples taken");
                continue;
            } else {
                //calculate number of negative examples allowed
                negMax = (positiveCount*negativeProportion);
                if (VERBOSE) {
                    System.out.println("Allowing up to " + negMax 
                                       + " negative examples");
                }
            }
    
            byte[] sentenceMeta = trainInst.getSentenceMeta().getBytes();
            byte[] sentenceText = trainInst.getSentenceText().getBytes();
            byte[] sentenceOffset = trainInst.getSentenceOffset().getBytes();
            byte[] tokenOffsets = trainInst.getTokenOffsets().getBytes();
            byte[] tokenPOSOut = trainInst.getTokenPOS().getBytes();
            byte[] tokenNEROut = trainInst.getTokenNER().getBytes();
            byte[] dependency = trainInst.getDependency().getBytes();
            
            int negCount = 0; 
            int negOdds = 1;
            if (negativeCount >= negMax){
                negOdds = negativeCount / negMax;
            }
            //add sampling of negatives to relations
            for (String negative : negatives){
                if (new Random().nextInt(negOdds) == 0 && negCount < negMax){
                        relations.add(negative);
                        negCount++;
                } else {
                    continue;
                }
            }
            if (VERBOSE){
                System.out.println(negCount + " negative examples taken");
            }

            //output one line per file per relation
            for (String rel : relations){
                meta.write(sentenceMeta); 
                meta.write(System.getProperty("line.separator").getBytes()); 
                text.write(sentenceText);
                text.write(System.getProperty("line.separator").getBytes());
                sentOffset.write(sentenceOffset);
                sentOffset.write(System.getProperty(
                                                 "line.separator").getBytes());
                tokenOffset.write(tokenOffsets);
                tokenOffset.write(System.getProperty(
                                                  "line.separator").getBytes());
                tokenNER.write(tokenNEROut);
                tokenNER.write(System.getProperty("line.separator").getBytes());
                tokenPOS.write(tokenPOSOut);
                tokenPOS.write(System.getProperty("line.separator").getBytes());
                sentDependency.write(dependency);
                sentDependency.write(System.getProperty(
                                     "line.separator").getBytes());
                instance.write(rel.getBytes());
                instance.write(System.getProperty("line.separator").getBytes());
            }
        }
    }

    /*
     * Close all the files.
     */
    private void closeFiles() throws IOException {
        try {
            meta.close();
            text.close();
            sentOffset.close();
            tokenOffset.close();
            tokenNER.close();
            sentDependency.close();
            tokenPOS.close();
            instance.close();
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Encountered IOException");
        }
    }

}
