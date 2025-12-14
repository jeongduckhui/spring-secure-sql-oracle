package com.example.demo.report.dto;


public class OrderByRequest {

    private String key;   // enum key
    private String dir;   // ASC / DESC

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
