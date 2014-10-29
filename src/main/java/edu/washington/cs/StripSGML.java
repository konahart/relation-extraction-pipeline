/* 
 * Reads through documents line by line to convert Gigaword corpus to 
 * AnnotatedDocs. Not recommended.
 * @author Laurel Hart
*/
package edu.washington.cs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class StripSGML implements CorpusConverter {
    public ArrayList<AnnotatedDoc> convert(File input) throws IOException {
    	BufferedReader reader = new BufferedReader(new FileReader(input));
        String docid = "";
        int startOffset = 0;
        int characterOffset = 0;

        ArrayList<AnnotatedDoc> allDocs = new ArrayList<AnnotatedDoc>();
        Pattern pattern = Pattern.compile("id=\"([^\"]*)\"");
        StringBuilder contents = new StringBuilder();
        String line = reader.readLine();
        while ((line != null)){
                characterOffset += line.length();
                if (line.startsWith("<DOC>")) {
                    //reset contents at start of new doc
                    contents.setLength(0);
                }
                else if (line.startsWith("<DOC ")) {
                    //reset contents at start of new doc
                    contents.setLength(0);
                    Matcher getDocID = pattern.matcher(line);
                    if (getDocID.find()) {
                        docid = getDocID.group(1);
                        System.out.println("DocID "+docid);
                    }
                    startOffset = characterOffset;
                }
                else if (line.startsWith("<DOCNO>")) {
                    String[] id = line.trim().split(" ");
                    docid = id[1];
                    System.out.println("DocID " + docid);
                    startOffset = characterOffset;
                }	
                //reached end of doc, add everything accumulated
                else if (line.startsWith("</DOC>")) {
                    AnnotatedDoc doc = new AnnotatedDoc(docid);
                    doc.addSentence(contents.toString(), startOffset, 
                                    startOffset+contents.length());
                    allDocs.add(doc);
                } 
                //ignore all other HTML lines
                else if (!line.startsWith("<")) {
                    contents.append(line);
                    contents.append(" ");
                }
                line = reader.readLine();
        }
        reader.close();
        return allDocs;
    }
    
    public void convert(File input, File output) throws Exception {
    // If input is a directory, process each file in directory
    	System.out.println("Processing");
    	FileOutputStream out = new FileOutputStream(output);
        if (input.canRead()){
            if (input.isDirectory()){
            	System.out.println("Directory");
                String[] documents = input.list();
                    for (int i = 0; i < documents.length; i++){
                        convert(new File(input, documents[i]), output);
                    }
            }
            else {
            	System.out.println("Found file");
            	ArrayList<AnnotatedDoc> newDocs = convert(input);
            	System.out.println(newDocs.toArray().length + " documents");
            	for (AnnotatedDoc doc : newDocs){
            		out.write(doc.toString().getBytes());
            		out.write("\r".getBytes());
            	}
            	out.close();
            }
        }
    }
}
