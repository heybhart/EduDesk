package com.example.myapplication;

public class TicketModel {
    private String ticketId;
    private String subject;
    private String category;
    private long createdAt;
    private String status;
    private String assignedName;
    private String assignedUid;

    public TicketModel(String ticketId, String subject, String category, String status, long createdAt, String assignedName, String assignedUid) {
        this.ticketId = ticketId;
        this.subject = subject;
        this.category = category;
        this.status = status;
        this.createdAt = createdAt;
        this.assignedName = assignedName;
        this.assignedUid = assignedUid;
    }

    public String getTicketId()  { return ticketId; }
    public String getSubject()   { return subject; }
    public String getCategory()  { return category; }
    public String getStatus()    { return status; }
    public long getCreatedAt()   { return createdAt; }
    public String getAssignedName() { return assignedName; }
    public String getAssignedUid() { return assignedUid; }
}
