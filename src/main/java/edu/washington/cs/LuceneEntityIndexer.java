/* 
 * Creates a Lucene index that can then be used for entity linking
 * Note: this class has not been extensively tested.
 * @see LuceneEntityLinker
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
 * Use lucene to build an index of entities to aliases
 */
public class LuceneEntityIndexer {

    private static final Version VERSION = Version.LUCENE_4_9;

    /*
     * Will usually be called on its own, not as part of the pipeline, 
     * because it only needs to be created once, or if the entity/alias 
     * pairing changes
     */
    public static void main (String[] args) throws IOException, ParseException{
        if (args.length < 2) {
            System.err.println("Required arguments: indexDirectory, "
                               + "aliasFile (aliasFile2)..."
                               + "(aliasFileN)");
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
     * @param aliasFile Expects a file with every line in the following format:
     *                  entity  alias
     *                  Can have more than one entity per alias, and more than 
     *                  one alias per entity, but each pairing is on its own 
     *                  line.
     *                  May be passed a directory, in which case it
     *                  recursively calles to add all files in directory
     *                  to the index
     */
    public static void buildIndex(IndexWriter indexWriter, File aliasFile)
                                                    throws IOException {
        if (aliasFile.canRead()){
            if (aliasFile.isDirectory()){
                String[] documents = aliasFile.list();
                if (documents != null) {
                    for (int i = 0; i < documents.length; i++) {
                        buildIndex(indexWriter, new File(aliasFile, 
                                   documents[i]));
                        System.out.println("Indexing "+documents[i]);
                    }
                }
            }
            else {
                BufferedReader reader = new BufferedReader(
                                        new FileReader(aliasFile));
                String path = aliasFile.getAbsolutePath();
                int docLine = 0;
                Pattern relationMatch = Pattern.compile(
                                                "([^\\t]+)\\t([^\\t]+)");
                String line = "";
                while ((line = reader.readLine()) != null){
                    Matcher getRel = relationMatch.matcher(line);
                    if (getRel.find()){
                        String entity = getRel.group(1);
                        System.out.println("Adding new entity1 "+entity);
                        String alias = getRel.group(2);
                        System.out.println("Adding new alias "+alias);
                        //store entity
                        Field entityField = new StringField("entity", entity, 
                                                        Field.Store.YES);
                        //store alias
                        Field aliasField = new StringField("alias", alias, 
                                                        Field.Store.YES);

                        //add entity-alias pairing to index
                        Document entityAlias = new Document();
                        entityAlias.add(entityField);
                        entityAlias.add(aliasField);
                        indexWriter.addDocument(entityAlias);
                    }
                }
                reader.close();
            }
        }
    }
}
