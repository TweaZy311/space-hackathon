package org.example.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseFromService {

    private boolean processFurther;
    private boolean planetIsEmpty;

}