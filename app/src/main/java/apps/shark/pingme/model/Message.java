package apps.shark.pingme.model;

/**
 * Created by Harsha on 7/18/2017.
 */

public class Message {

    private String sender;
    private String message;
    private Boolean multimedia = false;
    private String contentType = "";
    private String contentLocation = "";
    private String timestamp = "";
    private String datestamp = "";

    public Message(){

    }

    //Constructor for plain text message
    public Message(String sender, String message, String time, String date) {
        this.sender = sender;
        this.message = message;
        this.timestamp = time;
        this.multimedia = false;
        this.datestamp = date;
    }

    //Constructor for Multimedia message
    public Message(String sender, String message, String contentType, String contentLocation, String time, String date) {
        this.sender = sender;
        this.message = message;
        this.multimedia = true;
        this.contentType = contentType;
        this.timestamp = time;
        this.datestamp = date;
        this.contentLocation = contentLocation;
    }

    public String getSender() {
        return sender;
    }
    public String getTimestamp(){return timestamp;}
    public String getDatestamp(){return datestamp;}
    public String getMessage() {
        return message;
    }

    public String getContentLocation() {
        return contentLocation;
    }

    public Boolean getMultimedia() {
        return multimedia;
    }

    public String getContentType() {
        return contentType;
    }

}
