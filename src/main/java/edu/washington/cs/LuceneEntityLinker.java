/*
 * Uses a Lucene index to link mentions to entities
 * Note: this class has not been extensively tested.
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;

/*
 * @param MAXRELATIONS  Maximum number of relations for any ordered pair of 
 *                      entities
 *                      Required by IndexSearcher when performing search.
 *                      Made a field for each of editing, if need be.
 */
public class LuceneEntityLinker implements Linker {
    private IndexReader reader;
    private IndexSearcher searcher;
    private String outputExtension;
    private int MAXRELATIONS = 1000;
    private boolean VERBOSE = false;

    /*
     * Initialize with the default output extension
     */
    public LuceneEntityLinker(File indexDir) throws IOException{
        this(indexDir, ".linked");
    }

    public LuceneEntityLinker(File indexDir, String outputExt) 
                              throws IOException{
        reader = DirectoryReader.open(FSDirectory.open(indexDir));
        searcher = new IndexSearcher(reader);
    }

    public void setVerbose(boolean verbose){
        VERBOSE = verbose;
    }

    public static void main(String[] args) throws Exception {
        String outputExt = ".linked";
    	if ( args.length <= 2 ){
            System.err.println("Required arguments: input file or directory, "
                               + "output directory, relation index directory");
            System.err.println("output extension (optional)");
            System.err.println("Too few arguments supplied. Exiting.");
            System.exit(1);
        } else if (args.length >= 4){
            outputExt = "." + args[3];
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
    	File indexDirectory = new File(args[2]);

        Linker linker = new LuceneEntityLinker(indexDirectory);

        if (input.isDirectory()){
            File[] inputs = Utils.findFiles(input);
            for (File inputFile : inputs){
                linker.link(inputFile, output);
            }
        } else {
            linker.link(input, output);
        }
    }
 
    /*
     * Use the alias index to search for candidates for every entity mention
     * in every Sentence of an AnnotatedDoc.
     * If an entity has no candidates, remove the mention from the Sentence.
     * If the Sentence has fewer than 2 mentions, remove the Sentence from 
     * the AnnotatedDoc. 
     */
    public void link(AnnotatedDoc doc) throws IOException{
        System.out.println("Linking "+doc.getID());
        Iterator<Sentence> sentIter = doc.iterator(); 
        int mentionCount;
        while (sentIter.hasNext()){
            Sentence sentence = sentIter.next();
            mentionCount = 0;
            Collection<String> mentions = sentence.getMentionInstances();
            for (String mention : mentions){
                BooleanQuery booleanQuery = new BooleanQuery();
                Query alias = new TermQuery(new Term("alias", mention));

                booleanQuery.add(alias, BooleanClause.Occur.MUST);
                TopDocs entities = searcher.search(booleanQuery, 
                                                   MAXRELATIONS);

                System.out.println(entities.totalHits + " hits found for " 
                                   + alias); 
                ArrayList<String> entityCandidates = new ArrayList<String>();
                if (entities.totalHits > 0){
                    mentionCount++;
                    for (ScoreDoc scoreDoc : entities.scoreDocs){
                        Document d = searcher.doc(scoreDoc.doc);
                        String e = d.get("entity");
                        entityCandidates.add(e);
                        sentence.addCandidate(mention, e);
                    }
                } else {
                    sentence.removeMention(mention);
                }
            }
            if (mentionCount < 2){
                sentIter.remove();
            } 
        }
    }

    /*
     * Default output extension
     */ 
    public void link(File input, File outputDir) throws IOException {
        link(input, outputDir, outputExtension);
    }
    

    /*
     * Load ArrayList of Annotated docs, add entity candidates to them, and
     * output them again.
     */ 
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
