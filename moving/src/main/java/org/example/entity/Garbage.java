package org.example.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class Garbage {

    private Map<String, Map<Integer, Integer>> garbageId;

}