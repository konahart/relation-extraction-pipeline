/*
 * Object used by MultiR compiler to isolate it from document processing.
 * If MultiR output format changes, this is the Class to update.
 *
 * Formats :
 *
 * sentences.meta (sentenceMeta):
 * SentenceID   DocName Token1 Token2 Token3 ...
 *
 * SENTTEXTINFORMATION (sentenceText):
 * SentenceID   SentenceText 
 *
 * SENTOFFSETINFORMATION (sentenceOffset):
 * SentenceID   CharacterOffsetFromDocStart  CharacterOffsetFromDocEnd
 *
 * TOKENOFFSETINFORMATION (tokenOffsets):
 * SentenceID   Token1Start:Token1End Token2Start:Token2End ...
 *
 * TOKENPOSINFORMATION (tokenPOS):
 * SentenceID   Token1POS Token2POS Token3POS ...
 *
 * TOKENNERINFORMATION (tokenNER):
 * SentenceID   Token1NER Token2NER Token3NER ...
 *
 * SENTDEPENDENCYINFORMATION (dependency):
 * SentenceID   Span1StartTokenOffset Span1Type Span1EndTokenOffset | Span2...
 * 
 * training.inst (relations): 
 * Entity1ID    Entity1CharacterOffsetStart    Entity1CharacterOffsetEnd  Entity1MentionString    Entity2ID Entity2CharacterOffsetStart Entity2CharacterOffsetEnd   Entity2MentionString    SentenceID  Relation(or NA)
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.util.ArrayList;
/*
 * @param prefix            Most output starts with the same String 
 *                          sentenceID<tab>
 *                          so prebuild it when MultiRInstance is initialized. 
 * @param sentence          Stored in order to recover entity Strings 
 * @param sentenceMeta      sentences.meta output
 * @param sentenceText      SENTTEXTINFORMATION output
 * @param sentenceOffset    SENTOFFSETINFORMATION output
 * @param tokenOffsets      TOKENOFFSETINFORMATION output
 * @param tokenPOS          TOKENPOSINFORMATION output
 * @param tokenNER          TOKENNERINFORMATION output
 * @param dependency        SENTDEPENDENCYINFORMATION output
 * @param positives         All relations expressed by sentence
 * @param negatives         All negative relations expressed by sentence
 */
class MultiRInstance {
    public String documentID;
    private String prefix;
    public String sentence;
    public String sentenceMeta;
    public String sentenceText;
    public String sentenceOffset;
    public String tokenOffsets;
    public String tokenPOS;
    public String tokenNER;
    public String dependency;
    public ArrayList<String> positives;
    public ArrayList<String> negatives;

    public MultiRInstance(String docID, int sentenceNumber){
        sentence = "";
        sentenceMeta = "";
        sentenceText = "";
        sentenceOffset = "";
        tokenOffsets = "";
        tokenPOS = "";
        tokenNER = "";
        dependency = "";
        documentID = new String(docID);
        addSentenceID(docID, sentenceNumber);
        positives = new ArrayList<String>();
        negatives = new ArrayList<String>();
    }
    
    /*
     * Prebuild sentenceID prefix that will be used for most outputs
     * SentenceID\t
     */
    private void addSentenceID(String docID, int sentenceNumber){
        StringBuilder idBuilder = new StringBuilder();
        idBuilder.append(docID);
        idBuilder.append(".");
        idBuilder.append(sentenceNumber);
        idBuilder.append("\t");
        prefix = idBuilder.toString();
    }

    /* 
     * sentenceOffset output:
     * SentenceID\tDocStartOffset DocEndOffset
     */
    public void addSentenceOffset(int docStart, int docEnd){
        StringBuilder sentOffBuilder = new StringBuilder();
        sentOffBuilder.append(prefix);
        sentOffBuilder.append(docStart);
        sentOffBuilder.append(" ");
        sentOffBuilder.append(docEnd);
        sentenceOffset = sentOffBuilder.toString();
    }

    /* 
     * meta output:
     * SentenceID\tDocName\tToken1 Token2 Token3 ...
     */
    public void addTokens(String[] tokens){
        StringBuilder metaBuilder = new StringBuilder();             
        metaBuilder.append(prefix);
        metaBuilder.append(documentID);
        metaBuilder.append("\t");
        metaBuilder.append(spaceSeparatedBuilder(tokens));
        sentenceMeta = metaBuilder.toString();
    }

    /* 
     * text output
     * SentenceID\tSentenceText 
     * SentenceText is as it originally appeared, no processing, but no newlines
     */
    public void addSentence(String docSentence){
        sentence = docSentence;
        StringBuilder sentInfoBuilder = new StringBuilder();             
        sentInfoBuilder.append(prefix);
        sentInfoBuilder.append(docSentence);
        sentenceText = sentInfoBuilder.toString();
    }

    /* 
     * tokenPOS output
     * SentenceID\tToken1POS Token2POS ...
     */
    public void addPOS(String[] pos){
        StringBuilder posBuilder = new StringBuilder();             
        posBuilder.append(prefix);
        posBuilder.append(spaceSeparatedBuilder(pos));
        tokenPOS = posBuilder.toString();
    }

    /*
     * tokenNER output
     * SentenceID\tToken1NER Token2NER ...
     */
    public void addNER(String[] ner){
        StringBuilder nerBuilder = new StringBuilder();             
        nerBuilder.append(prefix);
        nerBuilder.append(spaceSeparatedBuilder(ner));
        tokenNER = nerBuilder.toString();
    }

    /*
     * Build space separated Strings from String[]
     */
    private String spaceSeparatedBuilder(String[] array){
        StringBuilder builder = new StringBuilder();
        int length = array.length;
        if (length < 1){
            return "";
        }
        builder.append(array[0]);
        for (int i=1; i<length; i++){
            builder.append(" ");
            builder.append(array[i]);
        }
        return builder.toString();
    }


    /* 
     * tokenOffset output
     * SentenceID\tToken1Start:Token1End Token2Start:Token2End ...
     */
    public void addTokenOffsets(String[] tokenStarts, String[] tokenEnds){
        int length = tokenStarts.length;
        if (length < 1){
            return;
        }

        StringBuilder builder = new StringBuilder();             
        builder.append(prefix);

        builder.append(tokenStarts[0]);
        builder.append(":");
        builder.append(tokenEnds[0]);

        for (int i=1; i<length; i++){
            builder.append(" ");
            builder.append(tokenStarts[i]);
            builder.append(":");
            builder.append(tokenEnds[i]);
        }
        tokenOffsets = builder.toString();
    }

    /* 
     * sentDependency output
     * SentenceID\tspan1StartTokenOffset span1Type span1EndTokenOffset | span2..
     */
    public void addDependency(String[] sentDep, String[] starts, 
                              String[] ends){
        int length = sentDep.length;
        if (length < 1){
            return;
        }
        
        StringBuilder sentDepBuilder = new StringBuilder();
        sentDepBuilder.append(prefix);

        sentDepBuilder.append(starts[0]);
        sentDepBuilder.append(" ");
        sentDepBuilder.append(sentDep[0]);
        sentDepBuilder.append(" ");
        sentDepBuilder.append(ends[0]);

        for (int i=1; i<length; i++){
            sentDepBuilder.append("|");
            sentDepBuilder.append(starts[i]);
            sentDepBuilder.append(" ");
            sentDepBuilder.append(sentDep[i]);
            sentDepBuilder.append(" ");
            sentDepBuilder.append(ends[i]);
        }
        dependency = sentDepBuilder.toString();
    }

     /* relation output:
     * Entity1ID\tEntity1Start\tEntity1End\tEntity1Mention\tEntity2ID\tEntity2Start\tEntity2End\tEntity2Mention\tRelation 
     */
    public void addRelation(String entity1, int e1Start, int e1End,
                            String entity2, int e2Start, int e2End,
                            String relation){
        StringBuilder relationBuilder = new StringBuilder();
        String entities = buildEntity(entity1, e1Start, e1End, entity2, e2Start, 
                                      e2End);
        relationBuilder.append(entities);
        relationBuilder.append(relation);
        positives.add(relationBuilder.toString());
    }

     /* relation output:
     * Entity1ID\tEntity1Start\tEntity1End\tEntity1Mention\tEntity2ID\tEntity2Start\tEntity2End\tEntity2Mention\tNA
     */
    public void addNegative(String entity1, int e1Start, int e1End,
                            String entity2, int e2Start, int e2End){
        StringBuilder relationBuilder = new StringBuilder();
        String entities = buildEntity(entity1, e1Start, e1End, entity2, 
                                      e2Start, e2End);
        relationBuilder.append(entities);
        relationBuilder.append("NA");
        negatives.add(relationBuilder.toString());
    }

    /*
     * Builds most of relation string, all but final relation.
     * Entity1ID\tEntity1Start\tEntity1End\tEntity1Mention\tEntity2ID\tEntity2Start\tEntity2End\tEntity2Mention\tSentenceID\tRelation
     */
    private String buildEntity(String entity1, int e1Start, int e1End,
                               String entity2, int e2Start, int e2End){
        String e1Mention;
        String e2Mention;
        try {
            e1Mention = sentence.substring(e1Start, e1End);
            e2Mention = sentence.substring(e2Start, e2End);
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Incorrect span given for mention");
            e1Mention = " ";
            e2Mention = " ";
        }
        StringBuilder relationBuilder = new StringBuilder();
        relationBuilder.append(entity1);
        relationBuilder.append("\t");
        relationBuilder.append(e1Start);
        relationBuilder.append("\t");
        relationBuilder.append(e1End);
        relationBuilder.append("\t");
        relationBuilder.append(e1Mention);
        relationBuilder.append("\t");
        relationBuilder.append(entity2);
        relationBuilder.append("\t");
        relationBuilder.append(e2Start);
        relationBuilder.append("\t");
        relationBuilder.append(e2End);
        relationBuilder.append("\t");
        relationBuilder.append(e2Mention);
        relationBuilder.append("\t");
        relationBuilder.append(prefix);
        return relationBuilder.toString();
    }

    public String getSentenceMeta(){
        return sentenceMeta;           
    }

    public String getSentenceText(){
        return sentenceText;
    }

    public String getSentenceOffset(){
        return sentenceOffset;
    }

    public String getTokenOffsets(){
        return tokenOffsets;
    }

    public String getTokenPOS(){
        return tokenPOS;
    }

    public String getTokenNER(){
        return tokenNER;
    }

    public String getDependency(){
        return dependency;
    }
    public ArrayList<String> getPositives(){
        return positives;
    }

    public ArrayList<String> getNegatives(){
        return negatives;
    }
}
