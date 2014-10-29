/*
 * Uses a Lucene index to search for relations.
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
public class LuceneRelationSearcher implements RelationAnnotator {
    private IndexReader reader;
    private IndexSearcher searcher;
    private String outputExtension;
    private int MAXRELATIONS = 1000;
    private boolean VERBOSE = false;

    /*
     * Initialize with the default output extension
     */
    public LuceneRelationSearcher(File indexDir) throws IOException{
        this(indexDir, ".relations");
    }

    public LuceneRelationSearcher(File indexDir, String outputExt) 
                                  throws IOException{
        reader = DirectoryReader.open(FSDirectory.open(indexDir));
        searcher = new IndexSearcher(reader);
        outputExtension = new String(outputExt);
    }

    public void setVerbose(boolean verbose){
        VERBOSE = verbose;
    }

    public static void main(String[] args) throws Exception {
        String outputExt = ".relations";
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

        RelationAnnotator annotator = new LuceneRelationSearcher(indexDirectory);
        try {
            if (input.isDirectory()){
                File[] inputs = Utils.findFiles(input);
                for (File inputFile : inputs){
                    annotator.annotate(inputFile, output);
                }
            } else {
                annotator.annotate(input, output);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    /*
     * Add relations to every Sentence of an AnnotatedDoc
     * For every entity in a sentence, check if it has a relation with any other
     * entity also found in sentence.
     * Every entity in a sentence may have multiple candidates.
     * If any of these candidates have one or more relations (hits), add 
     * only positive relations for that entity pair
     * If none of the candidates has a relation, add all negative (empty) 
     * relations for that pair. 
     * A sentence may have both positive and negative relations if it has more
     * than two entity mentions in it.
     * Note that the position of an entity in a relation is important.
     * e.g., "Sam is mother to Charlie" is not the same as 
     * "Charlie is mother to Sam"
     */
    public void annotate(AnnotatedDoc doc) throws IOException{
        Iterator<Sentence> docIter = doc.iterator(); 
        while (docIter.hasNext()){
            Sentence sentence = docIter.next();
            //get all mention strings in sentence
            Collection<Mention> mentions = sentence.getMentions().values();
            ArrayList<RelationAnnotation> negatives = 
                                            new ArrayList<RelationAnnotation>();
            ArrayList<RelationAnnotation> positives = 
                                            new ArrayList<RelationAnnotation>();
            //Typically the subject
            for (Mention entity1 : mentions){
                //Don't test against itself
                ArrayList<Mention> entity2s = new ArrayList<Mention>(mentions);
                entity2s.remove(entity1);
                int e1Start = entity1.spanStart;
                int e1End = entity1.spanEnd;
                //for each potential entity Mention1 represents:
                for (String e1 : entity1.getCandidates()){
                    //Typically the object
                    for (Mention entity2 : entity2s){
                        //for each potential entity Mention2 represents:
                        for (String e2 : entity2.getCandidates()){
                            BooleanQuery booleanQuery = new BooleanQuery();
                            Query qe1 = new TermQuery(new Term("entity1", e1));
                            Query qe2 = new TermQuery(new Term("entity2", e2));

                            booleanQuery.add(qe1, BooleanClause.Occur.MUST);
                            booleanQuery.add(qe2, BooleanClause.Occur.MUST);
                            TopDocs relations = searcher.search(booleanQuery, 
                                                                MAXRELATIONS);
                            if (VERBOSE){
                                System.out.println(relations.totalHits 
                                                   + " hits found for " + e1 
                                                   + " and " + e2);
                            }
                            ArrayList<String> rels = new ArrayList<String>();
                            if (relations.totalHits > 0){
                                for (ScoreDoc scoreDoc : relations.scoreDocs){
                                    Document d = searcher.doc(scoreDoc.doc);
                                    String rel = d.get("relation");
                                    rels.add(rel);
                                    if (VERBOSE){
                                        System.out.println(rel);
                                    }
                                }
                            }
                            RelationAnnotation relAnn = new RelationAnnotation(
                                                        rels, e1, e1Start, 
                                                        e1End, e2,
                                                        entity2.spanStart,
                                                        entity2.spanEnd);  
                            //Store up positives and negatives
                            //will only need to retain negative examples if 
                            //there are no positives for any two candidates
                            //in Mention pair 
                            //not yet added to Sentence
                            if (rels.size() > 0) {
                                positives.add(relAnn);
                            } else if (positives.size() == 0) {
                                negatives.add(relAnn);
                            }
                        } //end for every e2 candidate loop
                    } //end for every e2 Mention
                } //end for every e1 candidate loop 
                //once all candidates for entity pair have been tested, 
                //add all positives or all negatives if no positives exist
                if (positives.size() > 0){
                    for (RelationAnnotation relAnn : positives){
                        sentence.addRelationAnnotation(relAnn);
                    }
                } else {
                    for (RelationAnnotation relAnn : negatives){
                        sentence.addRelationAnnotation(relAnn);
                    }
                }
                positives.clear();
                negatives.clear();
            } //end for every e1 Mention
        } //end for AnnotatedDoc in ArrayList loop (iter)
    }

    /*
     * Use LuceneRelationSearcher's default output extension on output
     */ 
    public void annotate(File input, File outputDir) throws Exception {
        annotate(input, outputDir, outputExtension);
    }
    

    /*
     * Load ArrayList of Annotated docs, add RelaitonAnnotations to them, and
     * output them again.
     */ 
    public void annotate(File input, File outputDir, String extension) 
                         throws Exception {
        //read in AnnotatedDocs from file
        ArrayList<AnnotatedDoc> docs = Utils.deserializeAnnotatedDocs(input);
        //add relations to AnnotatedDocs
        for (AnnotatedDoc doc : docs){
            annotate(doc);
        }
        //write relation annotated AnnotatedDocs out
        String baseName = input.getName().replaceAll("\\..*$", "");
        String outName = new String(baseName + extension);
        File out = new File(outputDir, outName);
        Utils.serializeAnnotatedDocs(out, docs);
    }
}
