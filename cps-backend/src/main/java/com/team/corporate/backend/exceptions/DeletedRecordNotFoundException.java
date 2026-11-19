package com.team.corporate.backend.exceptions;

public class DeletedRecordNotFoundException extends RuntimeException {
    public DeletedRecordNotFoundException() {
        super("Запись не найдена.");
    }
}
