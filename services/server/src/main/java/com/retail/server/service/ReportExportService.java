package com.retail.server.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.retail.server.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 报表导出服务：把报表内容（Map 快照）渲染为 Excel(.xlsx) 与 PDF。
 */
@Slf4j
@Service
public class ReportExportService {

    /** 候选中文字体（按序探测系统字体，保证 PDF 中文不乱码）。 */
    private static final String[] CJK_FONT_CANDIDATES = {
            "C:/Windows/Fonts/msyh.ttc,0",
            "C:/Windows/Fonts/simsun.ttc,0",
            "C:/Windows/Fonts/simhei.ttf",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
            "/System/Library/Fonts/PingFang.ttc,0"
    };

    // ==================== Excel ====================

    @SuppressWarnings("unchecked")
    public byte[] toExcel(Map<String, Object> report) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(wb);

            // 概览
            Map<String, Object> overview = mapOf(report.get("overview"));
            Sheet ov = wb.createSheet("经营概览");
            writeKeyValue(ov, headerStyle, new Object[][]{
                    {"报表标题", str(report.get("title"))},
                    {"统计区间", str(report.get("periodStart")) + " ~ " + str(report.get("periodEnd"))},
                    {"生成时间", str(report.get("generatedAt"))},
                    {"营业额", overview.get("revenue")},
                    {"营业额环比增长额", overview.get("revenueGrowth")},
                    {"营业额环比增长率", ratePct(overview.get("revenueGrowthRate"))},
                    {"订单数", overview.get("orderCount")},
                    {"订单数环比增长率", ratePct(overview.get("orderCountGrowthRate"))},
                    {"客流量", overview.get("visitors")},
                    {"客流量环比增长率", ratePct(overview.get("visitorsGrowthRate"))},
                    {"商品销售总件数", overview.get("unitsSold")},
                    {"销量环比增长率", ratePct(overview.get("unitsSoldGrowthRate"))},
                    {"客单价", overview.get("avgOrderValue")},
                    {"取消订单数", overview.get("cancelledCount")},
                    {"取消订单金额", overview.get("cancelledAmount")}
            });

            writeTable(wb, headerStyle, "商品销售明细",
                    new String[]{"商品ID", "商品名称", "分类", "销量", "销售额", "占比(%)"},
                    listOf(report.get("goodsSales")),
                    new String[]{"goodsId", "name", "categoryName", "quantity", "amount", "ratio"});

            writeTable(wb, headerStyle, "分类销售",
                    new String[]{"分类", "销量", "销售额", "占比(%)"},
                    listOf(report.get("categorySales")),
                    new String[]{"name", "quantity", "amount", "ratio"});

            writeTable(wb, headerStyle, "销售趋势",
                    new String[]{"日期", "销售额", "订单数"},
                    listOf(report.get("trend")),
                    new String[]{"d", "sales", "orders"});

            Map<String, Object> inventory = mapOf(report.get("inventory"));
            writeTable(wb, headerStyle, "库存归零商品",
                    new String[]{"商品ID", "商品名称", "分类", "库存", "安全库存"},
                    listOf(inventory.get("zeroStock")),
                    new String[]{"goodsId", "name", "categoryName", "stock", "safeStock"});
            writeTable(wb, headerStyle, "急需补货商品",
                    new String[]{"商品ID", "商品名称", "分类", "库存", "安全库存"},
                    listOf(inventory.get("lowStock")),
                    new String[]{"goodsId", "name", "categoryName", "stock", "safeStock"});

            writeTable(wb, headerStyle, "补货建议",
                    new String[]{"商品ID", "商品名称", "分类", "当前库存", "安全库存", "本期销量", "建议补货量"},
                    listOf(report.get("replenishment")),
                    new String[]{"goodsId", "name", "categoryName", "stock", "safeStock", "soldInWindow", "suggestQty"});

            writeTable(wb, headerStyle, "滞销商品",
                    new String[]{"商品ID", "商品名称", "分类", "库存"},
                    listOf(report.get("slowGoods")),
                    new String[]{"goodsId", "name", "categoryName", "stock"});

            writeTable(wb, headerStyle, "异常告警",
                    new String[]{"商品名称", "告警类型", "内容", "状态", "时间"},
                    listOf(report.get("warnings")),
                    new String[]{"goodsName", "warningType", "warningMsg", "status", "createTime"});

            Map<String, Object> anomalies = mapOf(report.get("anomalies"));
            writeTable(wb, headerStyle, "异常库存调整",
                    new String[]{"商品名称", "类型", "变动量", "变动后库存", "备注", "时间"},
                    listOf(anomalies.get("inventoryAdjust")),
                    new String[]{"goodsName", "type", "changeAmount", "currentStock", "remark", "createTime"});

            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception ex) {
            log.error("导出 Excel 失败", ex);
            throw new BusinessException(500, "导出 Excel 失败: " + ex.getMessage());
        }
    }

    private void writeKeyValue(Sheet sheet, CellStyle headerStyle, Object[][] rows) {
        Row head = sheet.createRow(0);
        createCell(head, 0, "指标", headerStyle);
        createCell(head, 1, "数值", headerStyle);
        int r = 1;
        for (Object[] kv : rows) {
            Row row = sheet.createRow(r++);
            createCell(row, 0, kv[0], null);
            createCell(row, 1, kv[1], null);
        }
        sheet.setColumnWidth(0, 22 * 256);
        sheet.setColumnWidth(1, 30 * 256);
    }

    private void writeTable(Workbook wb, CellStyle headerStyle, String sheetName,
                            String[] headers, List<Map<String, Object>> rows, String[] keys) {
        Sheet sheet = wb.createSheet(safeSheetName(sheetName));
        Row head = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            createCell(head, i, headers[i], headerStyle);
        }
        int r = 1;
        for (Map<String, Object> row : rows) {
            Row dataRow = sheet.createRow(r++);
            for (int i = 0; i < keys.length; i++) {
                createCell(dataRow, i, row.get(keys[i]), null);
            }
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 18 * 256);
        }
    }

    private void createCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : value.toString());
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String safeSheetName(String name) {
        String cleaned = name.replaceAll("[\\\\/?*\\[\\]:]", " ");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    // ==================== PDF ====================

    @SuppressWarnings("unchecked")
    public byte[] toPdf(Map<String, Object> report) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, bos);
            document.open();

            BaseFont baseFont = loadCjkBaseFont();
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFont, 16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(baseFont, 12, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font cellFont = new com.lowagie.text.Font(baseFont, 9);
            com.lowagie.text.Font headFont = new com.lowagie.text.Font(baseFont, 9, com.lowagie.text.Font.BOLD);

            Paragraph title = new Paragraph(str(report.get("title")), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            Paragraph sub = new Paragraph(
                    "统计区间：" + str(report.get("periodStart")) + " ~ " + str(report.get("periodEnd"))
                            + "    生成时间：" + str(report.get("generatedAt")), cellFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(10f);
            document.add(sub);

            Map<String, Object> overview = mapOf(report.get("overview"));
            addSection(document, "一、经营概览", sectionFont);
            addTable(document, new String[]{"指标", "数值", "环比增长率"}, headFont, cellFont, List.of(
                    Arrays.asList("营业额", str(overview.get("revenue")), ratePct(overview.get("revenueGrowthRate"))),
                    Arrays.asList("订单数", str(overview.get("orderCount")), ratePct(overview.get("orderCountGrowthRate"))),
                    Arrays.asList("客流量", str(overview.get("visitors")), ratePct(overview.get("visitorsGrowthRate"))),
                    Arrays.asList("商品销售总件数", str(overview.get("unitsSold")), ratePct(overview.get("unitsSoldGrowthRate"))),
                    Arrays.asList("客单价", str(overview.get("avgOrderValue")), "-"),
                    Arrays.asList("取消订单", str(overview.get("cancelledCount")) + " 单 / ¥" + str(overview.get("cancelledAmount")), "-")
            ));

            addSection(document, "二、热销商品 TOP10", sectionFont);
            addTable(document, new String[]{"商品名称", "分类", "销量", "销售额", "占比%"}, headFont, cellFont,
                    pickRows(listOf(report.get("hotGoods")), new String[]{"name", "categoryName", "quantity", "amount", "ratio"}));

            addSection(document, "三、分类销售占比", sectionFont);
            addTable(document, new String[]{"分类", "销量", "销售额", "占比%"}, headFont, cellFont,
                    pickRows(listOf(report.get("categorySales")), new String[]{"name", "quantity", "amount", "ratio"}));

            Map<String, Object> inventory = mapOf(report.get("inventory"));
            addSection(document, "四、库存健康（库存归零 " + str(inventory.get("zeroStockCount"))
                    + " 项 / 急需补货 " + str(inventory.get("lowStockCount")) + " 项）", sectionFont);
            addTable(document, new String[]{"商品名称", "分类", "库存", "安全库存"}, headFont, cellFont,
                    pickRows(limit(listOf(inventory.get("lowStock")), 20),
                            new String[]{"name", "categoryName", "stock", "safeStock"}));

            addSection(document, "五、补货建议", sectionFont);
            addTable(document, new String[]{"商品名称", "当前库存", "安全库存", "本期销量", "建议补货量"}, headFont, cellFont,
                    pickRows(limit(listOf(report.get("replenishment")), 20),
                            new String[]{"name", "stock", "safeStock", "soldInWindow", "suggestQty"}));

            addSection(document, "六、异常与告警", sectionFont);
            addTable(document, new String[]{"商品名称", "告警类型", "内容", "时间"}, headFont, cellFont,
                    pickRows(limit(listOf(report.get("warnings")), 20),
                            new String[]{"goodsName", "warningType", "warningMsg", "createTime"}));

            document.close();
            return bos.toByteArray();
        } catch (Exception ex) {
            if (document.isOpen()) {
                document.close();
            }
            log.error("导出 PDF 失败", ex);
            throw new BusinessException(500, "导出 PDF 失败: " + ex.getMessage());
        }
    }

    private void addSection(Document document, String text, com.lowagie.text.Font font) throws Exception {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(8f);
        p.setSpacingAfter(4f);
        document.add(p);
    }

    private void addTable(Document document, String[] headers, com.lowagie.text.Font headFont,
                          com.lowagie.text.Font cellFont, List<List<String>> rows) throws Exception {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(new java.awt.Color(240, 240, 240));
            cell.setPadding(4f);
            table.addCell(cell);
        }
        if (rows.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("（无数据）", cellFont));
            empty.setColspan(headers.length);
            empty.setPadding(4f);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(empty);
        } else {
            for (List<String> row : rows) {
                for (String v : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(v == null ? "" : v, cellFont));
                    cell.setPadding(3f);
                    table.addCell(cell);
                }
            }
        }
        table.setSpacingAfter(6f);
        document.add(table);
    }

    private BaseFont loadCjkBaseFont() {
        for (String candidate : CJK_FONT_CANDIDATES) {
            try {
                String path = candidate.split(",")[0];
                if (!new File(path).exists()) {
                    continue;
                }
                return BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {
                // 尝试下一个候选字体
            }
        }
        try {
            // 兜底：内置字体（中文可能无法显示，但保证不抛异常）
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception ex) {
            throw new BusinessException(500, "PDF 字体加载失败: " + ex.getMessage());
        }
    }

    private List<List<String>> pickRows(List<Map<String, Object>> rows, String[] keys) {
        List<List<String>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<String> line = new ArrayList<>();
            for (String key : keys) {
                line.add(str(row.get(key)));
            }
            result.add(line);
        }
        return result;
    }

    private List<Map<String, Object>> limit(List<Map<String, Object>> rows, int max) {
        return rows.size() > max ? new ArrayList<>(rows.subList(0, max)) : rows;
    }

    // ==================== 工具 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapOf(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOf(Object o) {
        return o instanceof List ? (List<Map<String, Object>>) o : List.of();
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }

    /** 把增长率（0.1234）渲染为百分比文本（+12.34%）；null 表示基期为 0。 */
    private String ratePct(Object rate) {
        if (!(rate instanceof Number n)) {
            return "新增";
        }
        double pct = n.doubleValue() * 100;
        return (pct >= 0 ? "+" : "") + String.format("%.2f%%", pct);
    }
}
