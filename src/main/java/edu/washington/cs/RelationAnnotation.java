/* 
 * The among main datastructures the pipeline relies on to group and pass data.
 * @author Laurel Hart
 */
package edu.washington.cs;

import java.util.ArrayList;

public class RelationAnnotation {
    public String entity1;
    public int e1StartSpan;
    public int e1EndSpan;
    public String entity2;
    public int e2StartSpan;
    public int e2EndSpan;
    public ArrayList<String> relations;
    
    /*
     * Empty constructor exists mostly to allow for use of Jackson ObjectMapper
     * to de/serialize in JSON. Should probably not be used otherwise.
     * @see Utils#deserilaizeAnnotatedDoc(File)
     * @see Utils#deserilaizeAnnotatedDocs(File)
     */
    public RelationAnnotation(){
    }

    public RelationAnnotation(String e1, int e1s, int e1e, String e2, int e2s, 
                              int e2e) {
        entity1 = e1;
        e1StartSpan = e1s;
        e1EndSpan = e1e;
        entity2 = e2;
        e2StartSpan = e2s;
        e2EndSpan = e2e;
    }

    public RelationAnnotation(ArrayList<String> rel, String e1, int e1s, 
                              int e1e, String e2, int e2s, int e2e){
        relations = new ArrayList<String>();
        for (String r : rel){
            addRelation(r);
        }

        if (e1 != null){
            entity1 = e1;
        } else {
            entity1 = "";
        }

        e1StartSpan = e1s;
        e1EndSpan = e1e;

        if (e2 != null){
            entity2 = e2;
        } else {
            entity2 = "";
        }

        e2StartSpan = e2s;
        e2EndSpan = e2e;
    }

    public void addRelation(String relation){
        relations.add(new String(relation));
    }

    public ArrayList<String> getRelations(){
        return relations;
    }
}
