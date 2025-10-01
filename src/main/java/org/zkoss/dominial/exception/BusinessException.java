package org.zkoss.dominial.exception;


import java.util.ArrayList;
import java.util.List;

public class BusinessException extends RuntimeException{

    private List<String> errors;

    public List<String> getErrors() {
        if (errors==null) {
            errors = new ArrayList<>();
        }
        return errors;
    }

    public BusinessException() {
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(List<String> msg) {
        errors=msg;
    }


    public void addError(String errorMsg) {
        getErrors().add(errorMsg);
    }

    public boolean isMultiple() {
        return errors!=null && !errors.isEmpty();
    }
}
