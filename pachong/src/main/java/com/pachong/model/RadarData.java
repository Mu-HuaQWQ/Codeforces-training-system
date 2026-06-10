package com.pachong.model;

import java.util.List;

/**
 * 雷达图数据 — 一组标签和对应的数值
 */
public class RadarData {
    private String handle;           // 用户标识（系列名）
    private List<String> labels;     // 标签名称列表
    private List<Double> values;     // 对应数值列表

    public RadarData() {}

    public RadarData(String handle, List<String> labels, List<Double> values) {
        this.handle = handle;
        this.labels = labels;
        this.values = values;
    }

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }

    public List<Double> getValues() { return values; }
    public void setValues(List<Double> values) { this.values = values; }
}
