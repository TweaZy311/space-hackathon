package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class MainJson {

    private String name;
    private Ship ship;
    private List<List<Object>> universe;
//    private Garbage garbage;
    private Planet planet;

}
