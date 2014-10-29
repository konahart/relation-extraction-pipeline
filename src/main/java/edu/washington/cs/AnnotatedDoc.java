/*
 * Among the main datastructures the pipeline relies on to group and pass data.
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.util.ArrayList;
import java.util.Iterator;

public class AnnotatedDoc {
    private String docID;
    private ArrayList<Sentence> sentences;

    /*
     * Empty constructor exists mostly to allow for use of Jackson ObjectMapper
     * to de/serialize in JSON. Should probably not be used otherwise.
     * @see Utils#deserilaizeAnnotatedDoc(File)
     * @see Utils#deserilaizeAnnotatedDocs(File)
     */
    public AnnotatedDoc(){
        docID = "";
        sentences = new ArrayList<Sentence>();
    }

    public AnnotatedDoc(String id){
        docID = new String(id);
        sentences = new ArrayList<Sentence>();
    }

    public void setID(String id){
        docID = new String(id);
    }

    public void addSentence(String sentence, int startSpan, int endSpan){
        Sentence s = new Sentence(sentence, startSpan, endSpan);
        sentences.add(s);
    }
    
    public void addSentence(Sentence sentence){
        sentences.add(new Sentence(sentence));
    }

    public String getID(){
        return docID;
    }

    public ArrayList<Sentence> getSentences(){
        return sentences;
    }

    public int size(){
        return sentences.size();
    }

    public Iterator<Sentence> iterator(){
        return sentences.iterator();
    } 
}
