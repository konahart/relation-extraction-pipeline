/*
 * The among main datastructures the pipeline relies on to group and pass data.
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.util.ArrayList;
import java.util.HashMap;

/*
 *
 * @param start     Character offset in source document
 * @param end       Character offset in source document. Should probably be 
 *                  equal to start+sent.length(), but may not be if sent has 
 *                  undergone processing since extraction from document.
 * @param mentions  Store mentions by mention (sub)String in sentence. 
 *                  In future, might be better to use different key to avoid
 *                  reduplicating data (Strings) and to allow for more than
 *                  just String matching of entities to aliases.
 * @param relations List of relations present in sentence.
 *                  Note: data structure currently duplicates data present in
 *                  mentions.
 */
public class Sentence {
    private String sentence;
    private int startSpan;
    private int endSpan;
    private HashMap<String, Mention> mentions;
    private ArrayList<RelationAnnotation> relations;
    
    /*
     * Empty constructor exists mostly to allow for use of Jackson ObjectMapper
     * to de/serialize in JSON. Should probably not be used otherwise.
     * @see Utils#deserilaizeAnnotatedDoc(File)
     * @see Utils#deserilaizeAnnotatedDocs(File)
     */
    public Sentence(){
        sentence = "";
        startSpan = 0;
        endSpan = 0;
        mentions = new HashMap<String, Mention>();
        relations = new ArrayList<RelationAnnotation>();
    }
   
    public Sentence(Sentence old){
        sentence = new String(old.sentence);
        startSpan = old.startSpan;
        endSpan = old.endSpan;
        mentions = new HashMap<String, Mention>();
        mentions.putAll(old.mentions);
        relations = new ArrayList<RelationAnnotation>();
    }

    /*
     * Initialize Sentence with empty set of mentions and relations. 
     */
    public Sentence(String sent, int start, int end){
        sentence = new String(sent);
        startSpan = start;
        endSpan = end;
        mentions = new HashMap<String, Mention>();
        relations = new ArrayList<RelationAnnotation>();
    }
    
    public void setStart(int start){
        startSpan = start;
    }

    public void setEnd(int end){
        endSpan = end;
    }
    
    /*
     * Warning: if sentence is set/changed, startSpan and endSpan probably
     * also need to be set/changed
     */
    public void setSentence(String sent){
        sentence = new String(sent);
        if (endSpan == 0){
            endSpan = sentence.length() + startSpan;
        }
    }

    public void addMention(String mentionString, int start, int end){
        if (!mentions.containsKey(mentionString)){
            Mention mention = new Mention(start, end);
            mentions.put(mentionString, mention);
        }
    }

    public void addMention(String mentionString, int start, int end, 
                           String type){
        if (!mentions.containsKey(mentionString)){
            Mention mention = new Mention(start, end, type);
            mentions.put(mentionString, mention);
        }
    }

    public void removeMention(String mentionString){
        mentions.remove(mentionString);
    }

    public void addCandidate(String mention, String candidate){
        if (mentions.containsKey(mention)){
            mentions.get(mention).addCandidate(candidate);
        }
    }

    /* 
     * Need to find a way to combine relations with the same e1 and e2 
     */
    public void addRelationAnnotation(RelationAnnotation relation){
        relations.add(relation); 
    }

    /* 
    * Need to find a way to combine relations with the same e1 and e2
    */
    public void addRelationAnnotation(ArrayList<String> relation, 
                                      String entity1, int e1s, int e1e, 
                                      String entity2, int e2s, int e2e){
        relations.add(new RelationAnnotation(relation, entity1, e1s, e1e, 
                                             entity2, e2s, e2e)); 
    }

    public String getSentence(){
        return sentence;
    }

    public int getStart(){
        return startSpan;
    }

    public int getEnd(){
        return endSpan;
    }

    /* 
    * Used to get just the mention strings in the sentence, presumably for use
    * to link mentions to entities.
    */
    public ArrayList<String> getMentionInstances(){
        return new ArrayList<String>(mentions.keySet());
    }
    
    public HashMap<String, Mention> getMentions(){
        return mentions;
    }

    public ArrayList<RelationAnnotation> getRelations(){
        return relations;
    }
}
