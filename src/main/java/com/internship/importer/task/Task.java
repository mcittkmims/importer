package com.internship.importer.task;

import java.io.IOException;

public interface Task {
    TaskStatus getStatus();
    void setToIncompleteStatus();
    void setToCompleteStatus();
    void execute();
}
