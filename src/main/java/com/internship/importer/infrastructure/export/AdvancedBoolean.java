package com.internship.importer.infrastructure.export;

import lombok.Getter;
import lombok.Setter;

public class AdvancedBoolean{
    private Boolean condition;

    public AdvancedBoolean(){
        this.condition = false;
    }

    public void setTrue(){
        condition = true;
    }

    public void setFalse(){
        condition = false;
    }

    public Boolean getCondition(){
        return condition;
    }



}
