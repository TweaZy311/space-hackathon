package org.example.entity;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseFromService {

    @SerializedName("ProcessFurther")
    private boolean processFurther;
    private boolean planetIsEmpty;
    private String occupancy;

}