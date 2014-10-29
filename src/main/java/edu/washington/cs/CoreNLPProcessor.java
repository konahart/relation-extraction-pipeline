/*
 * Use Stanford's CoreNLP toolkit to annotate documents.
 * @author Laurel Hart
 */

package edu.washington.cs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;


public class CoreNLPProcessor implements DocProcessor{
    private String outputExtension;
    private StanfordCoreNLP processor;
    private boolean VERBOSE = false;
    private boolean XMLOUTPUT = false;
    private boolean pre = true;
    private File xmlOutputDir;
    private GZIPOutputStream XMLOutput;

    /*
     * Initialize with default output extension
     */
    public CoreNLPProcessor(){
        this(".processed");
    }

    public CoreNLPProcessor(String outputExt){
        outputExtension = new String(outputExt);

        //set up processing so it will only need to be loaded once
        //preprocessing: sentence splitting, tokenization, NER
        Properties properties = new Properties();
        properties.put("annotators", "tokenize, ssplit, pos, lemma, ner");
        processor = new StanfordCoreNLP(properties);
    }

    public void setVerbose(boolean verbose){
        VERBOSE = verbose;
    }

    public static void main(String[] args){
    	if ( args.length < 2 ){
            System.err.println("Required arguments: input file or directory, "
                               + "output directory");
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
        DocProcessor docProc = new CoreNLPProcessor();
        try {
            docProc.preprocess(input, output); 
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    /*
     * Set whether to write the CoreNLP XML out. 
     * Will be written to subdirectory of output directory called "xml"
     */
    public void setXMLOutput(boolean output){
        XMLOUTPUT = output;
    }

    /* 
     * Process one File at a time.
     * One File may contain multiple articles.
     * Ensures all sentences are longer than 5 tokens, shorter than 50 tokens,
     * end with punctuation, and have at least 2 named entities. 
     */
    public ArrayList<AnnotatedDoc> preprocess(File doc) throws Exception{
        //Strip docs of SGML
        //Note that SAXGigawordConverter has not performed sentence splitting
        //on text, so Sentences in AnnotatedDocs are actually paragraphs from
        //the original doc
        SAXGigawordConverter stripSGML = new SAXGigawordConverter();
        stripSGML.setVerbose(VERBOSE);
        ArrayList<AnnotatedDoc> articles = stripSGML.convert(doc); 
        if (VERBOSE){
            System.out.println(articles.size() + " articles found.");
        }

        Iterator<AnnotatedDoc> articleIter = articles.iterator(); 
        AnnotatedDoc article;
        //for each document found in File
        while (articleIter.hasNext()){
            article = articleIter.next();

            //prep to output all the document's good sentences
            if (XMLOUTPUT){
                String xmlName = new String(article.getID() + ".xml.gz");
                XMLOutput = new GZIPOutputStream(new FileOutputStream(
                            new File(xmlOutputDir, xmlName)));
            }

            ArrayList<Sentence> newSentences = new ArrayList<Sentence>();
            Iterator<Sentence> paragraphIter = article.iterator(); 
            Sentence paragraphSentence;
            //separate article paragraphs into sentences
            while (paragraphIter.hasNext()){
                paragraphSentence = paragraphIter.next();
                String paragraphString = paragraphSentence.getSentence(); 
                int paragraphStartOffset = paragraphSentence.getStart(); 

                //split sentences and tokenize each paragraph
                Annotation paragraph = new Annotation(paragraphString); 
                processor.annotate(paragraph);
                List<CoreMap> sentences = paragraph.get(
                    SentencesAnnotation.class);

                //check that each sentence is well-formed
                //eg, 6 <= words >= 50
                for(CoreMap sentence : sentences) {
                    //check number of tokens
                    List<CoreLabel> tokens = 
                        sentence.get(TokensAnnotation.class);
                    int tokenCount = tokens.size();
                    if (tokenCount < 6){
                        if (VERBOSE){
                            System.out.println("Removing invalid sentence "
                                    + "from " + article.getID() 
                                    + " (too few tokens)");
                        }
                        continue;
                    } else if (tokenCount > 51){
                        if (VERBOSE){
                            System.out.println("Removing invalid sentence "
                                    + "from " + article.getID() 
                                    + " (too many tokens)");
                        }
                        continue;
                    } 
                    
                    int sentenceStart = sentence.get(
                                         CharacterOffsetBeginAnnotation.class);
                    int sentenceEnd = sentence.get(
                                            CharacterOffsetEndAnnotation.class);
                    //create new sentence to add Mentions to
                    //CharacterOffsetBegin/EndAnnotation is relative to String
                    //that was annotated, so need to add to paragraph's offset
                    Sentence newSentence = new Sentence(sentence.toString(), 
                                                        sentenceStart
                                                        + paragraphStartOffset,
                                                        sentenceEnd
                                                        + paragraphStartOffset);
                    //check that there are at least 2 entity mentions
                    int entCount = 0;
                    //build up entity strings by concatenating adjacent tokens
                    //of same entity type 
                    //note that this approach will not capture (_ of the _)
                    StringBuilder entityBuilder = new StringBuilder();
                    String entityType = "O";
                    int beginPosition = 0;
                    int endPosition = 0;

                    for (CoreLabel token : tokens){
                        String entity = token.get(
                            NamedEntityTagAnnotation.class);
                        if (entity.equals("LOCATION") 
                            || entity.equals("ORGANIZATION") 
                         // || entity.equals("DATE") //ignore DATES for now
                            || entity.equals("PERSON")){
                            //start a new entity string
                            if (entityBuilder.length() == 0){
                                //begin/endPosition is relative to String
                                //that was annotated, but need relative to
                                //current sentence
                                //so need to add to paragraph's offset
                                beginPosition = token.beginPosition() 
                                                - sentenceStart;
                                endPosition = token.endPosition()
                                              - sentenceStart;
                                entityBuilder.append(token.toString());
                                entityType = new String(entity);
                            } else if (entity.equals(entityType)){
                                //combine spans that are the same NER type 
                                entityBuilder.append(token.before());
                                entityBuilder.append(token.toString());
                                endPosition = token.endPosition()
                                              - sentenceStart;
                            } else {
                                //different NER type, finish ongoing string
                                String mention = entityBuilder.toString();
                                if (VERBOSE){
                                    System.out.println("Entity found: " 
                                        + entityType + " " + mention); 
                                }
                                newSentence.addMention(mention, 
                                                       beginPosition, 
                                                       endPosition,
                                                       entityType);
                                entCount++;

                                //start new one with new type
                                entityBuilder.setLength(0);
                                entityBuilder.append(token.toString());
                                beginPosition = token.beginPosition()
                                                - sentenceStart;
                                endPosition = token.endPosition()
                                                - sentenceStart;
                                entityType = new String(entity);
                            }
                        //if not a special NER type, finish building entity
                        //note that this approach will not capture (_ of the _)
                        } else if (entityBuilder.length() > 0) {
                            String mention = entityBuilder.toString();
                            if (VERBOSE){
                                System.out.println("Entity found: " 
                                    + entityType + " " + mention); 
                            }
                            newSentence.addMention(mention, 
                                                   beginPosition, 
                                                   endPosition,
                                                   entityType);
                            entCount++;
                            entityBuilder.setLength(0);
                        }
                    }
                    if (entCount < 2){
                        continue;
                    }
                    newSentences.add(newSentence);
                } // end for sentence in paragraph loop
                //remove old paragraph -- it will be replaced with newSentences
                paragraphIter.remove();

                //print paragraph xml to xml output
                if (XMLOUTPUT){
                    processor.xmlPrint(paragraph, XMLOutput);
                }
            } //end paragraph in article loop
            //add new, improved sentences to article
            for (Sentence newSentence : newSentences){
                article.addSentence(newSentence);
            }
            //close XML out
            if (XMLOUTPUT){
                XMLOutput.close();
            }
            
            //if there is at least one valid sentence in the doc, store it
            if (article.size() < 0){
                if (VERBOSE) {
                    System.out.println("Removing document " + article.getID()
                                       + " (no valid sentences)");
                }
                articleIter.remove();
            }
        } //end document in array loop
        return articles;
    }

    /* 
     * Call single-doc version of process, write to file in output 
     */
    public void preprocess(File input, File output) throws Exception {
        if (XMLOUTPUT){
            xmlOutputDir = new File(output, "/xml");
            if (!xmlOutputDir.exists()){
                xmlOutputDir.mkdir();
            }
            if (VERBOSE){
                System.out.println("Outputting XML to " + xmlOutputDir);
            }
        }
        ArrayList<AnnotatedDoc> docs = preprocess(input);
        int validDocs = docs.size();
        if (validDocs < 1) {
            //don't bother doing output if there are no valid documents
            return;
        }
        String baseName = input.getName().replaceAll("\\..*$", "");
        String outName = new String(baseName + outputExtension);
        File out = new File(output, outName);
        Utils.serializeAnnotatedDocs(out, docs);
    }

    /* 
     * Switch from pre- to post- processing.
     * Post processing may have more annotations to capture.
     */
    private void setProcessing(){
        if (pre){
            if (VERBOSE){
                System.out.println("Prepping annotator for MultiR processing");
            }
            //add dependency parse annotator
            processor.addAnnotator(new ParserAnnotator(false, 100000));

            //if possible, find way to replace ssplit annotator with 
            //nonsplitting, since sentences should already be split
            //processor.addAnnotator(WordsToSentencesAnnotator.nonSplitter(
            //                       VERBOSE));
            pre = false;
            if (VERBOSE){
                System.out.println("Done prepping");
            }
        }
    }

    /* 
     * Prepare documents for MultiR output.
     * Call single AnnotatedDoc process.
     * @see process(AnnotatedDoc)
     */
    public ArrayList<MultiRInstance> process(File input) throws Exception {
        setProcessing();
        ArrayList<MultiRInstance> multiR = new ArrayList<MultiRInstance>();
        try { 
            if (VERBOSE){
                System.out.println("Reading annnotated docs in from " + input);
            }
            //read in AnnotatedDocs from file
            ArrayList<AnnotatedDoc> docs = Utils.deserializeAnnotatedDocs(
                                           input);
            Iterator<AnnotatedDoc> docIter = docs.iterator(); 
            while (docIter.hasNext()){
                AnnotatedDoc doc = docIter.next();
                ArrayList<MultiRInstance> multis = process(doc);
                for (MultiRInstance inst : multis){
                    multiR.add(inst);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Encountered IOException");
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Encountered Exception");
        }
        return multiR;
    }

    /* 
     * Prepare documents for MultiR output.
     */
    private ArrayList<MultiRInstance> process(AnnotatedDoc doc) 
                                             throws Exception {
        ArrayList<MultiRInstance> multis = new ArrayList<MultiRInstance>();

        String docID = doc.getID();
        if (VERBOSE){
            System.out.println("Processing " + docID + " for MultiR");
        }
        ArrayList<Sentence> docSentences = doc.getSentences();

        //prepare to capture annotation information
        ArrayList<String> allTokens = new ArrayList<String>();
        ArrayList<String> tokenPOSs = new ArrayList<String>();
        ArrayList<String> tokenNERs = new ArrayList<String>();
        ArrayList<String> tokenOffsetStarts = new ArrayList<String>();
        ArrayList<String> tokenOffsetEnds = new ArrayList<String>();
        ArrayList<String> sentDep = new ArrayList<String>();
        ArrayList<String> sentDepStarts = new ArrayList<String>();
        ArrayList<String> sentDepEnds = new ArrayList<String>();
        
        int sentenceNum = 0;
        for (Sentence docSentence : docSentences){
            int docOffset = docSentence.getStart();
            int endOffset = docSentence.getEnd();

            Annotation annotation = processor.process(docSentence.getSentence());
            List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
            int start;
            int end;
            for (CoreMap sentence : sentences) {
                List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
                for (CoreLabel token : tokens) {
                    allTokens.add(token.get(TextAnnotation.class));
                    tokenPOSs.add(token.get(PartOfSpeechAnnotation.class));
                    tokenNERs.add(token.get(NamedEntityTagAnnotation.class));

                    start = token.beginPosition();
                    end = token.endPosition();
                    tokenOffsetStarts.add(String.valueOf(start));
                    tokenOffsetEnds.add(String.valueOf(end));
                }

                SemanticGraph dependencies = sentence.get(
                                            BasicDependenciesAnnotation.class);
                for (IndexedWord root : dependencies.getRoots()) {
                    sentDep.add("root");
                    sentDepStarts.add("0");
                    sentDepEnds.add(String.valueOf(root.index()));
                }
                for (SemanticGraphEdge edge : dependencies.edgeListSorted()) {
                    sentDep.add(edge.getRelation().toString());
                    sentDepStarts.add(String.valueOf(edge.getTarget().index()));
                    sentDepEnds.add(String.valueOf(edge.getSource().index()));
                }
            }

            MultiRInstance multiR = new MultiRInstance(docID, sentenceNum);
            //pass annotation information to MultiR instance
            //MultiR instance will take care of MultiR formatting 
            multiR.addSentence(docSentence.getSentence());
            multiR.addSentenceOffset(docOffset, endOffset);
            multiR.addTokens(allTokens.toArray(new String[allTokens.size()]));
            multiR.addNER(tokenNERs.toArray(new String[tokenNERs.size()]));
            multiR.addPOS(tokenPOSs.toArray(new String[tokenPOSs.size()]));
            multiR.addTokenOffsets(tokenOffsetStarts.toArray(
                                   new String[tokenOffsetStarts.size()]), 
                                   tokenOffsetEnds.toArray(
                                   new String[tokenOffsetEnds.size()]));
            multiR.addDependency(sentDep.toArray(
                                 new String[sentDep.size()]),
                                 sentDepStarts.toArray(
                                 new String[sentDepStarts.size()]),
                                 sentDepEnds.toArray(
                                 new String[sentDepEnds.size()]));

            //now add all the relations (training instances) MultiR needs 
            ArrayList<RelationAnnotation> relations = 
                docSentence.getRelations();
            for (RelationAnnotation relation : relations){
                String e1 = relation.entity1;
                int e1Start = relation.e1StartSpan;
                int e1End = relation.e1EndSpan;
                String e2 = relation.entity2;
                int e2Start = relation.e2StartSpan;
                int e2End = relation.e2EndSpan;
                ArrayList<String> rels = relation.relations;

                //sort into positive and negative relations
                if (rels.size() > 0){
                    for (String rel : rels){
                        multiR.addRelation(e1, e1Start, e1End, e2, e2Start, 
                                           e2End, rel); 
                    }
                } else {
                        multiR.addNegative(e1, e1Start, e1End, e2, e2Start, 
                                           e2End); 
                }
            }
            multis.add(multiR);
            sentenceNum++;
        }
        return multis;   
    }
}
