/* 
 * Creates a Lucene index that can then be used for relation annotation
 * @see LuceneRelationSearcher
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/*
 * Use lucene to build an index of relations we care about
 */
public class LuceneRelationIndexer {

    private static final Version VERSION = Version.LUCENE_4_9;

    /*
     * Will usually be called on its own, not as part of the pipeline, 
     * because it only needs to be created once, or if the entity/alias 
     * pairing changes
     */
    public static void main (String[] args) throws IOException, ParseException{
        if (args.length < 2) {
            System.err.println("Required arguments: indexDirectory, "
                               + "relationFile (relationFile2)..."
                               + "(relationFileN)");
            System.err.println("Too few arguments supplied. Exiting.");
            System.exit(0);
        }
        File indexDir = new File(args[0]);

        Directory index = FSDirectory.open(indexDir);
        Analyzer analyzer = new StandardAnalyzer(VERSION);

        IndexWriterConfig config = new IndexWriterConfig(VERSION,analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        config.setRAMBufferSizeMB(7168);
        IndexWriter indexWriter = new IndexWriter(index,config);

        for (int i = 1; i < args.length; i++){
            buildIndex(indexWriter, new File(args[i]));
        }
        indexWriter.commit();
        indexWriter.close();
    }

    /*
     * @param relationsFile Expects a file with every line in the following 
     *                      format:
     *                      entity1 relation  entity2
     *                      May be passed a directory, in which case it
     *                      recursively calles to add all files in directory
     *                      to the index
     *                      This is useful if each type of relation is stored
     *                      in its own file
     */
    public static void buildIndex(IndexWriter indexWriter, File relationsFile)
                                                    throws IOException {
        HashSet<String> relations = new HashSet<String>();
        if (relationsFile.canRead()){
            if (relationsFile.isDirectory()){
                String[] documents = relationsFile.list();
                if (documents != null) {
                    for (int i = 0; i < documents.length; i++) {
                        buildIndex(indexWriter, new File(relationsFile, 
                                   documents[i]));
                        System.out.println("Indexing "+documents[i]);
                    }
                }
            }
            else {
                BufferedReader reader = new BufferedReader(
                                        new FileReader(relationsFile));
                String path = relationsFile.getAbsolutePath();
                int docLine = 0;
                Pattern relationMatch = Pattern.compile(
                                        "([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)");
                String line = "";
                while ((line = reader.readLine()) != null){
                    Matcher getRel = relationMatch.matcher(line);
                    if (getRel.find()){
                        String e1 = getRel.group(1);
                        System.out.println("Adding new entity1 "+e1);
                        String rel = getRel.group(2);
                        String e2 = getRel.group(3);
                        System.out.println("Adding new entity2 "+e2);
                        if (!relations.contains(rel)){
                            relations.add(rel);
                            System.out.println("Adding new relation "+rel);
                        }

                        //store entity1
                        Field e1Field = new StringField("entity1", e1, 
                                                        Field.Store.YES);
                        //store relation
                        Field relField = new StringField("relation", rel, 
                                                        Field.Store.YES);
                        //store entity2
                        Field e2Field = new StringField("entity2", e2, 
                                                        Field.Store.YES);

                        //add fact triplet to index
                        Document relationTuple = new Document();
                        relationTuple.add(e1Field);
                        relationTuple.add(relField);
                        relationTuple.add(e2Field);
                        indexWriter.addDocument(relationTuple);
                    }
                }
                reader.close();
            }
        }
    }
}
