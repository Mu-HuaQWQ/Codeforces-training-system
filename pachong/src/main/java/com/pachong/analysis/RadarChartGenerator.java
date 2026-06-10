package com.pachong.analysis;

import com.pachong.model.RadarData;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.DefaultCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 雷达图生成器 — 使用JFreeChart SpiderWebPlot
 */
public class RadarChartGenerator {
    private static final Logger log = LoggerFactory.getLogger(RadarChartGenerator.class);

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    // 预定义的系列颜色
    private static final Color[] SERIES_COLORS = {
        new Color(41, 128, 185, 180),   // 蓝
        new Color(231, 76, 60, 180),    // 红
        new Color(39, 174, 96, 180),    // 绿
        new Color(243, 156, 18, 180),   // 橙
        new Color(142, 68, 173, 180),   // 紫
        new Color(52, 152, 219, 180),   // 浅蓝
        new Color(230, 126, 34, 180),   // 浅橙
        new Color(46, 204, 113, 180),   // 浅绿
    };

    private Font titleFont;
    private Font labelFont;
    private Font legendFont;

    public RadarChartGenerator() {
        initFonts();
    }

    private void initFonts() {
        // 尝试加载中文字体
        String[] fontCandidates = {
            "Microsoft YaHei", "SimHei", "Noto Sans CJK SC",
            "WenQuanYi Micro Hei", "Source Han Sans SC", "SansSerif"
        };

        for (String fontName : fontCandidates) {
            Font f = new Font(fontName, Font.PLAIN, 12);
            if (f.canDisplayUpTo("能力雷达图") == -1) {
                labelFont = new Font(fontName, Font.PLAIN, 12);
                titleFont = new Font(fontName, Font.BOLD, 18);
                legendFont = new Font(fontName, Font.PLAIN, 11);
                log.debug("Using font: {}", fontName);
                return;
            }
        }

        // 兜底
        labelFont = new Font("SansSerif", Font.PLAIN, 12);
        titleFont = new Font("SansSerif", Font.BOLD, 18);
        legendFont = new Font("SansSerif", Font.PLAIN, 11);
    }

    /**
     * 生成雷达图并返回BufferedImage（供GUI使用）
     */
    public BufferedImage generate(List<RadarData> datasets, String title) {
        DefaultCategoryDataset categoryDataset = buildDataset(datasets);

        SpiderWebPlot plot = new SpiderWebPlot(categoryDataset);
        configurePlot(plot, datasets);

        JFreeChart chart = new JFreeChart(title, titleFont, plot, true);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(legendFont);
        chart.getLegend().setPosition(RectangleEdge.BOTTOM);

        return chart.createBufferedImage(WIDTH, HEIGHT);
    }

    /**
     * 生成雷达图并保存为PNG文件
     */
    public void generateAndSave(List<RadarData> datasets, String title, Path outputPath)
            throws IOException {
        BufferedImage image = generate(datasets, title);
        Files.createDirectories(outputPath.getParent());
        ChartUtils.saveChartAsPNG(outputPath.toFile(),
            buildChart(datasets, title), WIDTH, HEIGHT);
        log.info("Radar chart saved to {}", outputPath);
    }

    /**
     * 构建JFreeChart对象
     */
    public JFreeChart buildChart(List<RadarData> datasets, String title) {
        DefaultCategoryDataset categoryDataset = buildDataset(datasets);
        SpiderWebPlot plot = new SpiderWebPlot(categoryDataset);
        configurePlot(plot, datasets);

        JFreeChart chart = new JFreeChart(title, titleFont, plot, true);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(legendFont);
        chart.getLegend().setPosition(RectangleEdge.BOTTOM);
        return chart;
    }

    // === 私有方法 ===

    private DefaultCategoryDataset buildDataset(List<RadarData> datasets) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (RadarData data : datasets) {
            for (int i = 0; i < data.getLabels().size(); i++) {
                dataset.addValue(
                    data.getValues().get(i),
                    data.getHandle(),
                    data.getLabels().get(i)
                );
            }
        }

        return dataset;
    }

    private void configurePlot(SpiderWebPlot plot, List<RadarData> datasets) {
        plot.setStartAngle(90);
        plot.setLabelFont(labelFont);
        plot.setOutlinePaint(Color.GRAY);
        plot.setAxisLinePaint(new Color(200, 200, 200));
        plot.setLabelPaint(Color.DARK_GRAY);
        plot.setWebFilled(true);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setBackgroundAlpha(0.05f);

        // 计算最大值（取所有数据的最大值，向上取整到最近的整数）
        double maxVal = 1.0; // 至少为1，避免空图
        for (RadarData data : datasets) {
            for (Double val : data.getValues()) {
                if (val > maxVal) maxVal = val;
            }
        }
        // 向上取整到最近的偶数或5的倍数
        maxVal = Math.ceil(maxVal / 5.0) * 5.0;
        if (maxVal == 0) maxVal = 5.0;
        plot.setMaxValue(maxVal);

        // 设置系列颜色
        for (int i = 0; i < datasets.size(); i++) {
            Color color = SERIES_COLORS[i % SERIES_COLORS.length];
            plot.setSeriesPaint(i, color);
            plot.setSeriesOutlinePaint(i, color.darker());
            plot.setSeriesOutlineStroke(i, new BasicStroke(2.0f));
        }

        // 网格线样式
        plot.setAxisLineStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND, 0, new float[]{2, 4}, 0));
    }
}
