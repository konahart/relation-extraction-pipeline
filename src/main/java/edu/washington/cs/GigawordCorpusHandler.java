/*
 * Handler used by SAXGigawordCorpusConverter to parse Gigaword documents. 
 * Note that because of the way SAX splits up character() calls, offsets
 * given by this class are incorrect.
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Parses Gigaword corpus into AnnotatedDocs, which can then be retrieved using
 * getText().
 * Ignore documents that are not "story" type.
 */
public class GigawordCorpusHandler extends DefaultHandler {
    /*
     * @param storyFlag Signal whether currently in an article of type "story"
     *                  Only want "story" type text.
     * @param textFlag  Signal whether currently within a <TEXT> field.
     *                  Do not want headlines, dates, etc.  
     */     
    private boolean VERBOSE = false;
    private boolean docFlag = false;
    private boolean storyFlag = false;
    private boolean textFlag = false;
    private boolean paraFlag = false;
    private String docID;
    private ArrayList<AnnotatedDoc> collectedText;
    private AnnotatedDoc currentDoc;
    private StringBuilder currentParagraph;
    private int offset = 0;
    private int currentDocStart = 0;
    private int currentParaStart = 0;
    
    public GigawordCorpusHandler(){
        collectedText = new ArrayList<AnnotatedDoc>();
        currentParagraph = new StringBuilder();
        docID = "";
    }

    public void setVerbose(boolean verbose){
        VERBOSE = verbose;
    }

    /* 
     *  Called on encountering a starting tag, e.g., <DOC> or <TEXT> 
    */
    public void startElement(String uri, String localName, String qName, 
        Attributes attributes) throws SAXException {
            if (VERBOSE){
                System.out.println(qName);
            }
            
            if (qName.equalsIgnoreCase("DOC")){
                String type = attributes.getValue("type");
                String id = attributes.getValue("id");
                
                if (type.equalsIgnoreCase("story")){
                    storyFlag = true;
                    docID = id;
                    currentDoc = new AnnotatedDoc(id);
                    docFlag = true;
                    if (VERBOSE) {
                        System.out.println("New doc " + id + " started.");
                    } 
                } else {
                    storyFlag = false;
                    if (VERBOSE) {
                        System.out.println("Non-story encountered..." + type);
                    }
                }
            
            } else if (qName.equalsIgnoreCase("TEXT")){
                textFlag = true;
            } else if (qName.equalsIgnoreCase("P")){
                paraFlag = true;
            }
    }

    /* 
     * Build up all text between <TEXT> and </TEXT> from doc of type story
     * However, if <P> </P> is inside <TEXT> </TEXT>, need to record only
     * text between <P> tags
     */
    public void characters(char[] ch, int start, int length) 
                           throws SAXException {
        //nothing to do here if doc type is not story
        if (!storyFlag){
            return;
        }
        if (textFlag){
            if (paraFlag){
                //<P> is inside text, so clear currentParagraph
                currentParagraph.setLength(0);
                textFlag = false;
            }
            //set starting offset 
            if (currentParagraph.length() == 0){
                currentParaStart = start - currentDocStart; 
            }
            String line = new String(ch, start, length);
            currentParagraph.append(line);
            if (VERBOSE) {
                System.out.println(line);
                System.out.println("Text between " + currentParaStart
                                   + " and " 
                                   + (currentParaStart + line.length()) 
                                   + " added.");
            }
        } else if (paraFlag) {
            String line = new String(ch, start, length);
            currentParaStart = start - currentDocStart; 
            currentParagraph.append(line);
            if (VERBOSE) {
                System.out.println(line);
                System.out.println("Text between " + currentParaStart
                                   + " and " 
                                   + (currentParaStart + line.length()) 
                                   + " added.");
            }
        } else if (docFlag){
            //if starting a new doc and not yet reached the text section
            docFlag = false;
            if (currentParagraph.length() == 0){
                //25 = length of <DOC id="" type="story" >
                currentDocStart = start;
                //currentDocStart = start - 25 - docID.length();
                if (VERBOSE) {
                    System.out.println("Doc " + docID + " starts at " 
                    + currentDocStart);
                }
            }
        }
    }

    /*
     * Called on encountering a end tag, e.g., </DOC> or </TEXT> 
     * If doc has paragraphs (<P>), save each paragraph as a Sentence.
     * Otherwise, save the entire <TEXT> as a single Sentence.
     * Sentence splitting will be performed by another processor.
     * When the document is finished, store in AnnotatedDoc then clear 
     * out StringBuilder.
     */
    public void endElement(String uri, String localName, String qName){
        if (VERBOSE){
            System.out.println("/" + qName);
        }
        //</P>
        if (qName.equalsIgnoreCase("P")) {
            String para = currentParagraph.toString();
            Sentence sent = new Sentence(para, currentParaStart, 
                                         currentParaStart +
                                         para.length());
            currentDoc.addSentence(sent);
            //reset paragraph builder
            currentParagraph.setLength(0);
            paraFlag = false;
        //</TEXT>
        } else if (qName.equalsIgnoreCase("TEXT")) {
            //check paragraph builder isn't empty from <P> ending
            if (currentParagraph.length() > 0){
                String para = currentParagraph.toString();
                Sentence sent = new Sentence(para, currentParaStart, 
                                             para.length());
                currentDoc.addSentence(sent);
                //reset paragraph builder
                currentParagraph.setLength(0);
            }
            textFlag = false;
        //</DOC>
        } else if (qName.equalsIgnoreCase("DOC") && storyFlag){
            //add complete AnnotatedDoc
            collectedText.add(currentDoc);
            if (VERBOSE) {
                System.out.println("Document " + docID + " completed.");
            }
            docID = "";
        }
    }
    
    public void setOffset(int off){
        offset = off;
    }

    /*
     * @return  AnnotedDocs created from parsed document.
     */
    public ArrayList<AnnotatedDoc> getText(){
        return collectedText;
    }

    public void clear(){
        collectedText.clear();
    }
}
