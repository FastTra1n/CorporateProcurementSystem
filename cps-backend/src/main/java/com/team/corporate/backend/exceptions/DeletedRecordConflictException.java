package com.team.corporate.backend.exceptions;

import com.team.corporate.backend.dto.DeletedRecordReference;

import java.util.List;

public class DeletedRecordConflictException extends RuntimeException {
    private final List<DeletedRecordReference> dependencies;

    public DeletedRecordConflictException(String message, List<DeletedRecordReference> dependencies) {
        super(message);
        this.dependencies = List.copyOf(dependencies);
    }

    public List<DeletedRecordReference> getDependencies() {
        return dependencies;
    }
}
