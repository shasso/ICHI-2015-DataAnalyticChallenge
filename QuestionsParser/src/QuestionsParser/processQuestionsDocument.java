package QuestionsParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author sargon.hasso
 */
public class processQuestionsDocument {

    private final String InputFile;
    private int count;

    public processQuestionsDocument(String input) {
        InputFile = input;
        count = 0;
    }

    public ArrayList<Question> InputToDocumentsList() {
        ArrayList<Question> data = new ArrayList<>();
        try {
            try (FileReader reader = new FileReader(InputFile)) {
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line;
                String id = "";
                String topic = "";
                StringBuilder tempBuffer = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    // System.out.println(line);
                    if (line.length() <= 2) {
                        continue;       // skip blank lines: usually LF+CR
                    }
                    // parse the string
                    // header line
                    int left = line.indexOf("[");
                    int right = line.indexOf("]");
                    if (left == -1 || right == -1) {
                        tempBuffer.append(line.trim());
                        
                    } else {
                        if (id.length() > 0) {
                            // flush buffer
                            Question q = new Question(id, topic, tempBuffer.toString());
                            data.add(q);
                            // System.out.println(createXMLDoc(q));
                            count++;
                            tempBuffer = new StringBuilder(); // re-init buffer
                        }
                        // extract question ID
                        id = line.substring(left + 1, right);
                        // extract topic & tighten the string
                        topic = (line.substring(right + 1)).trim();
                    }
                }
                // clean up before shutting down
                if (id.length() > 0) {
                    // flush buffer
                    Question q = new Question(id, topic, tempBuffer.toString());
                    data.add(q);
                    
                    count++;
                }
                reader.close();
                // System.out.println(count);
            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        return (data);
    }

    public void InputToXMLDoc(String output, ArrayList<Question> qList) {
        // convert list of Questions to List of Strings
        ArrayList<String> data = new ArrayList<>();
        for (Question q : qList) {
            data.add(createXMLDoc(q));
        }
        writeToFile(output, data);
    }

    public String createXMLDoc(Question q) {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        result.append("<doc>").append(NEW_LINE);
        result.append("<field name=\"id\">").append(q.ID).append("</field>").append(NEW_LINE);
        result.append("<field name=\"topic\">").append(q.Topic).append("</field>").append(NEW_LINE);
        result.append("<field name=\"body\">").append(q.Body).append("</field>").append(NEW_LINE);
        result.append("</doc>").append(NEW_LINE);
        return (result.toString());
    }

    public void writeToFile(String f, ArrayList<String> questions) {

        String NEW_LINE = System.getProperty("line.separator");
        // make it legitimate xml
        try (FileWriter writer = new FileWriter(f, true)) {
            // make it legitimate xml
            writer.write("<add>");
            writer.write(NEW_LINE);   // write new line
            for (String q : questions) {
                writer.write(q);
                writer.write(NEW_LINE);   // write new line
            }
            writer.write("</add>");
            writer.write(NEW_LINE);   // write new line

        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    public int NumProcessedLines() {
        return (count);
    }

}
