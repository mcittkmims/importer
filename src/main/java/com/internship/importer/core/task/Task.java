package com.internship.importer.core.task;

public interface Task {
    TaskStatus getStatus();

    void setToIncompleteStatus();

    void setToCompleteStatus();

    void execute();
}
