package QuestionsParser;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author sargon.hasso
 */
public class Question {
    public String ID;
    public String Topic;
    public String Body;
    public Question(String id, String topic, String body) {
        this.ID = id;
        this.Topic = topic;
        this.Body = body;
        }

    /**
     *
     * @return
     */
    @Override public String toString() {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        result.append("ID: " + ID + NEW_LINE);
        result.append("Topic: " + Topic + NEW_LINE);
        result.append("Body: " + Body + NEW_LINE);
        return(result.toString());       
    }
}
