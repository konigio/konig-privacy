package io.konig.privacy.deidentification.service;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException{
	private static final long serialVersionUID = 1L;
	
	public NotFoundException(String Exception){
		super(Exception);
	}

}
