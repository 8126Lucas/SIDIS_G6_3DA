package com.sidis.maintenanceservice.domain;

import java.util.Map;

public class DoneListRedis {
    private String completionNotes;
    private Map<String, Boolean> tasksDone;

    public DoneListRedis(String completionNotes, Map<String,Boolean> tasksDone) {
        this.completionNotes = completionNotes;
        this.tasksDone = tasksDone;
    }

    public DoneListRedis() {
    }

    public String getCompletionNotes() {
        return completionNotes;
    }

    public Map<String, Boolean> getTasksDone() {
        return tasksDone;
    }
}