package com.studenthub.app.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class AppNotification {
    private String id;
    private String title;
    private String body;
    private String text;
    private String toUid;
    private Date createdAt;

    public AppNotification() {}
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getBody(){return body;} public void setBody(String v){body=v;}
    public String getText(){return text;} public void setText(String v){text=v;}
    public String getToUid(){return toUid;} public void setToUid(String v){toUid=v;}
    @ServerTimestamp public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
}
