package com.nikhilm.hourglass.tidbitsservice.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TidbitException extends RuntimeException{
    private int status;
    private String message;
}
