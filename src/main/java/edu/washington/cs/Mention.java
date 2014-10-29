/* 
 * @author Laurel Hart 
 */
package edu.washington.cs;

import java.util.HashSet;

/*
 * @param spanStart     Starting character offset with respect to sentence in
 *                      which it appeats
 * @param spanEnd       Ending character offset with respect to the sentence
 *                      in which it appears.
 *                      Will typically be equal to spanStart + mention length
 * @param type          Somewhat optional field indicating the mention's NER
 *                      label type, e.g., PERSON, LOCATION, etc.
 * @param candidates    Set of entities which may be signified by the mention
 */ 
public class Mention {
    public int spanStart;
    public int spanEnd;
    public String type;
    public HashSet<String> candidates;

    /*
     * Empty constructor exists mostly to allow for use of Jackson ObjectMapper
     * to de/serialize in JSON. Should probably not be used otherwise.
     * @see Utils#deserilaizeAnnotatedDoc(File)
     * @see Utils#deserilaizeAnnotatedDocs(File)
     */
    public Mention(){
        candidates = new HashSet<String>();
    }
    
    public Mention(int start, int end){
        spanStart = start;
        spanEnd = end;
        candidates = new HashSet<String>();
    }

    public Mention(int start, int end, String category){
        spanStart = start;
        spanEnd = end;
        type = new String(category);
        candidates = new HashSet<String>();
    }

    public Mention(Mention old){
        spanStart = old.spanStart;
        spanEnd = old.spanEnd;
        type = new String(old.type);
        candidates = new HashSet<String>();
        for (String candidate : old.candidates){
            candidates.add(new String(candidate));
        }
    }
    
    public void addCandidate(String candidate){
        candidates.add(new String(candidate));
    }

    public void removeCandidate(String candidate){
        candidates.remove(candidate);
    }
    
    public int getSpanStart(){
        return spanStart;
    }

    public int getSpanEnd(){
        return spanEnd;
    }

    public String getType(){
        return type;
    }

    public HashSet<String> getCandidates(){
        return candidates;
    }
}
