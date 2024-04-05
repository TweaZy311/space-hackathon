package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MainJson {

    private String name;
    private Ship ship;
    private Universe universe;
    private Garbage garbage;

}
