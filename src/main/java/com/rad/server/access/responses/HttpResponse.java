package com.rad.server.access.responses;

import com.rad.server.access.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HttpResponse {



    private Exception error = null;

    private String message = null;

    private HttpStatus httpStatusCode;

    private ResponseEntity<?> responseEntity = null;

    public HttpResponse(HttpStatus status,String message){
        this.httpStatusCode = status;
        this.message = message;
    }

    public HttpResponse(ResponseEntity<?> result) {
        this.responseEntity = result;
    }


    public ResponseEntity<?> getHttpResponse()

    {

        if (responseEntity != null) return responseEntity;

        return new ResponseEntity<String>(getMessage(),getHttpStatusCode());

    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public HttpStatus getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(HttpStatus httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public ResponseEntity<?> getResponseEntity() {
        return responseEntity;
    }

    public void setResponseEntity(ResponseEntity<?> responseEntity) {
        this.responseEntity = responseEntity;
    }
}
