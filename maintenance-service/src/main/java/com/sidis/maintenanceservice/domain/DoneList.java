package com.sidis.maintenanceservice.domain;

import jakarta.persistence.*;

import java.util.Map;

@Embeddable
public class DoneList {
    private String completionNotes;

    @ElementCollection
    @CollectionTable(name = "done_list_tasks")
    @MapKeyColumn(name = "task")
    @Column(name = "completed")
    private Map<String, Boolean> tasksDone;
    public DoneList(String completionNotes, Map<String,Boolean> tasksDone) {
        this.completionNotes = completionNotes;
        this.tasksDone = tasksDone;
    }

    public DoneList() {
    }

    public String getCompletionNotes() {
        return completionNotes;
    }

    public Map<String, Boolean> getTasksDone() {
        return tasksDone;
    }
}