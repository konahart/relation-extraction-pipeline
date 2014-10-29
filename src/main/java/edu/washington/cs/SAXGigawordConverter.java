/* 
 *  Uses Java SAX parser to convert Gigaword documents to AnnotatedDocs
 *  @author Laurel Hart
 */
package edu.washington.cs;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXGigawordConverter implements CorpusConverter {
    private boolean VERBOSE = false;

    public void setVerbose(boolean verbose){
        VERBOSE = verbose;
    }

    public static void main(String[] args){
    	if ( args.length < 1 ){
            System.err.println("Required arguments: input file.");
            System.err.println("Too few arguments supplied. Exiting.");
            System.exit(1);
        }
    	File input = new File(args[0]);
        
        CorpusConverter cc = new SAXGigawordConverter();

        try {
            cc.convert(input); 
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public ArrayList<AnnotatedDoc> convert(File input) throws IOException, 
                                SAXException, ParserConfigurationException {
        
    	SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        GigawordCorpusHandler handler = new GigawordCorpusHandler();
        handler.setVerbose(VERBOSE);

        // Try parsing, but Gigaword docs usually need a root added
        try {
            InputStream inputStream = Utils.gzipCheck(input);
            InputStreamReader reader = new InputStreamReader(inputStream);
            InputSource is = new InputSource(reader);
            parser.parse(is, handler);
            inputStream.close();
        } catch (SAXException ex){
            //add a root and try to parse again
            try {
                InputStream inputStream = Utils.gzipCheck(input);
                InputStreamReader reader = addRoot(inputStream);
                InputSource is = new InputSource(reader);
                //get rid of collected text from previous attempt to parse
                handler.clear();
                //added <ROOT> to beginning of document, so need to adjust
                //offsets
                handler.setOffset(6);
                parser.parse(is, handler);
                inputStream.close();
            } catch (Exception rootFail){
            //if adding a root doesn't help, give up.
                System.out.println("Cannot parse this document due to " + rootFail);
                ArrayList<AnnotatedDoc> newDoc = new ArrayList<AnnotatedDoc>();
                return newDoc;
            }
        } catch (Exception ex){
            System.out.println("Cannot parse this document due to " + ex);
            ArrayList<AnnotatedDoc> newDoc = new ArrayList<AnnotatedDoc>();
            return newDoc;
        }

        ArrayList<AnnotatedDoc> docs = handler.getText();

        return docs;
    }
    
    /*
     */
    public void convert(File input, File output) throws Exception {
    }

    /* 
     * Gigaword documents need to have a root added for SAX to accept them.
     */   
    public InputStreamReader addRoot(InputStream fileInput) throws IOException{
        byte[] fakeRootOpen = "<ROOT>".getBytes();
        ByteArrayInputStream rootOpen = new ByteArrayInputStream(fakeRootOpen);
        byte[] fakeRootClose = "</ROOT>".getBytes();
        ByteArrayInputStream rootClose = new ByteArrayInputStream(fakeRootClose);
        SequenceInputStream seq = new SequenceInputStream(rootOpen, fileInput);
        SequenceInputStream inputStream = new SequenceInputStream(seq, rootClose);

        InputStreamReader reader = new InputStreamReader(inputStream);

        return reader;
    }
}
