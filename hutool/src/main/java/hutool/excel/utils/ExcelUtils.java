package hutool.excel.utils;

import basic.clazz.ClassUtils;
import cn.hutool.core.io.IoUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.poi.excel.cell.CellUtil;
import cn.hutool.poi.excel.style.StyleUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excel工具类，提供Excel文件的读写、导入导出功能
 *
 * @author pengshuaifeng
 * @since 2023/12/30
 */
public class ExcelUtils {

    private ExcelUtils() {
        // Utility class should not be instantiated
    }

    /** 默认Sheet索引 */
    private static final int DEFAULT_SHEET_INDEX = 0;

    /** 默认起始行索引 */
    private static final int DEFAULT_START_ROW = 1;

    /** 默认起始列索引 */
    private static final int DEFAULT_START_COLUMN = 0;

    /** 默认表头行索引 */
    private static final int DEFAULT_HEADER_ROW = 0;

    /** 样式缓存，提高性能 */
    private static final Map<String, CellStyle> STYLE_CACHE = new ConcurrentHashMap<>();

    /** 错误消息 */
    private static final String EXPORT_ERROR = "Excel导出失败: ";
    private static final String IMPORT_ERROR = "Excel导入失败: ";

    /**
     * 导出数据到Excel
     * 
     * @param headers 表头映射（中文名:属性名）
     * @param rows 数据行
     * @param out 输出流
     * @param isXlsx 是否为xlsx格式
     * @param mergeModels 合并单元格配置
     * @throws RuntimeException 如果导出失败
     * @author pengfulin
     * @since 2022/11/23 16:31
     */
    public static void export(Map<String, String> headers, List<?> rows, OutputStream out, 
            boolean isXlsx, List<MergeModel> mergeModels) {
        validateExportParams(headers, rows, out);

        try {
            ExcelWriter writer = writeRow(headers, rows, isXlsx);
            if (mergeModels != null && !mergeModels.isEmpty()) {
                merge(writer, mergeModels);
            }
            flush(writer, out);
        } catch (Exception e) {
            throw new RuntimeException(EXPORT_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 导出数据到Excel（自动生成表头）
     * 
     * @param ignoreFields 需要忽略的字段
     * @param rows 数据行
     * @param out 输出流
     * @param isXlsx 是否为xlsx格式
     * @param mergeModels 合并单元格配置
     * @throws RuntimeException 如果导出失败
     */
    public static void exportNoHead(Set<String> ignoreFields, List<?> rows, OutputStream out, 
            boolean isXlsx, List<MergeModel> mergeModels) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("数据行不能为空");
        }
        export(defineHeaders(rows.get(0).getClass(), ignoreFields), rows, out, isXlsx, mergeModels);
    }

    /**
     * 使用模板导出数据到Excel
     * 
     * @param templatePath 模板路径（类路径）
     * @param headers 表头字段列表
     * @param rows 数据行
     * @param startRowIndex 起始行索引
     * @param startColumnIndex 起始列索引
     * @param out 输出流
     * @param mergeModels 合并单元格配置
     * @throws RuntimeException 如果导出失败
     */
    public static void exportByTemplate(String templatePath, List<String> headers, List<?> rows,
            int startRowIndex, int startColumnIndex, OutputStream out, List<MergeModel> mergeModels) {
        InputStream template = ExcelUtils.class.getResourceAsStream(templatePath);
        if (template == null) {
            throw new IllegalArgumentException("找不到模板文件: " + templatePath);
        }
        exportByTemplate(template, headers, rows, startRowIndex, startColumnIndex, out, mergeModels);
    }

    /**
     * 使用模板导出数据到Excel
     * 
     * @param template 模板输入流
     * @param headers 表头字段列表
     * @param rows 数据行
     * @param startRowIndex 起始行索引
     * @param startColumnIndex 起始列索引
     * @param out 输出流
     * @param mergeModels 合并单元格配置
     * @throws RuntimeException 如果导出失败
     */
    public static void exportByTemplate(InputStream template, List<String> headers, List<?> rows,
            int startRowIndex, int startColumnIndex, OutputStream out, List<MergeModel> mergeModels) {
        validateTemplateParams(template, headers, rows, out);

        try (Workbook workbook = WorkbookFactory.create(template)) {
            Sheet sheet = workbook.getSheetAt(DEFAULT_SHEET_INDEX);
            
            // 获取或创建样式
            CellStyle mergeCellStyle = getOrCreateStyle(workbook, "merge", true);
            CellStyle defaultCellStyle = getOrCreateStyle(workbook, "default", false);

            // 写入数据
            writeDataToSheet(sheet, headers, rows, startRowIndex, startColumnIndex, defaultCellStyle);

            // 合并单元格
            if (mergeModels != null && !mergeModels.isEmpty()) {
                merge(sheet, mergeModels, mergeCellStyle);
            }

            // 输出
            workbook.write(out);
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException(EXPORT_ERROR + e.getMessage(), e);
        } finally {
            IoUtil.close(out);
        }
    }

    /**
     * 从Excel导入数据（默认配置）
     * 
     * @param in Excel输入流
     * @param targetType 目标类型
     * @return 导入的数据集合
     * @throws RuntimeException 如果导入失败
     */
    public static <T> Collection<T> read(InputStream in, Class<T> targetType) {
        try {
            return read(in, targetType, DEFAULT_SHEET_INDEX, DEFAULT_HEADER_ROW, DEFAULT_START_ROW, null);
        } catch (Exception e) {
            throw new RuntimeException(IMPORT_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 从Excel导入数据（指定Sheet名）
     * 
     * @param in Excel输入流
     * @param targetType 目标类型
     * @param sheetName Sheet名称
     * @return 导入的数据集合
     * @throws RuntimeException 如果导入失败
     */
    public static <T> Collection<T> read(InputStream in, Class<T> targetType, String sheetName) {
        try {
            return read(in, targetType, sheetName, DEFAULT_HEADER_ROW, DEFAULT_START_ROW, null);
        } catch (Exception e) {
            throw new RuntimeException(IMPORT_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 从Excel导入数据（带字段映射）
     * 
     * @param in Excel输入流
     * @param targetType 目标类型
     * @param targetFields 字段映射（字段名:表头名）
     * @return 导入的数据集合
     * @throws RuntimeException 如果导入失败
     */
    public static <T> Collection<T> read(InputStream in, Class<T> targetType, 
            Map<String, String> targetFields) {
        try {
            return read(in, targetType, DEFAULT_SHEET_INDEX, DEFAULT_HEADER_ROW, DEFAULT_START_ROW, 
                targetFields);
        } catch (Exception e) {
            throw new RuntimeException(IMPORT_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 从Excel导入数据（完整参数）
     * 
     * @param in Excel输入流
     * @param targetType 目标类型
     * @param sheetIndex Sheet索引
     * @param headerRowIndex 表头行索引
     * @param startRowIndex 数据起始行索引
     * @param targetFields 字段映射（字段名:表头名）
     * @return 导入的数据集合
     * @throws RuntimeException 如果导入失败
     */
    public static <T> Collection<T> read(InputStream in, Class<T> targetType, int sheetIndex,
            int headerRowIndex, int startRowIndex, Map<String, String> targetFields) {
        validateImportParams(in, targetType);

        try {
            ExcelReader reader = ExcelUtil.getReader(in, sheetIndex);
            return readData(reader, targetType, headerRowIndex, startRowIndex, targetFields);
        } catch (Exception e) {
            throw new RuntimeException(IMPORT_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 从Excel导入数据（指定Sheet名）
     * 
     * @param in Excel输入流
     * @param targetType 目标类型
     * @param sheetName Sheet名称
     * @param headerRowIndex 表头行索引
     * @param startRowIndex 数据起始行索引
     * @param targetFields 字段映射（字段名:表头名）
     * @return 导入的数据集合
     * @throws RuntimeException 如果导入失败
     */
    public static <T> Collection<T> read(InputStream in, Class<T> targetType, String sheetName,
            int headerRowIndex, int startRowIndex, Map<String, String> targetFields) {
        validateImportParams(in, targetType);

        try {
            ExcelReader reader = ExcelUtil.getReader(in, sheetName);
            return readData(reader, targetType, headerRowIndex, startRowIndex, targetFields);
        } catch (Exception e) {
            throw new RuntimeException(IMPORT_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 生成表头映射
     * 
     * @param type 目标类型
     * @param ignoreFields 需要忽略的字段
     * @return 表头映射（字段名:表头名）
     */
    private static Map<String, String> defineHeaders(Class<?> type, Collection<String> ignoreFields) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (Field field : type.getDeclaredFields()) {
            String name = field.getName();
            if (ignoreFields != null && ignoreFields.contains(name)) {
                continue;
            }
            // TODO: 可以通过注解获取字段的中文名
            headers.put(name, name);
        }
        return headers;
    }

    /**
     * 写入数据到Sheet
     */
    private static void writeDataToSheet(Sheet sheet, List<String> headers, List<?> rows,
            int startRowIndex, int startColumnIndex, CellStyle defaultCellStyle) {
        int currentRow = Math.max(1, startRowIndex);
        int currentColumn = Math.max(0, startColumnIndex);

        for (Object row : rows) {
            Row excelRow = sheet.createRow(currentRow);
            for (String header : headers) {
                Cell cell = excelRow.createCell(currentColumn);
                Object value = ClassUtils.getFieldValue(header, row);
                writeCellValue(cell, value, defaultCellStyle);
                currentColumn++;
            }
            currentColumn = Math.max(0, startColumnIndex);
            currentRow++;
        }
    }

    /**
     * 写入单元格值
     */
    private static void writeCellValue(Cell cell, Object value, CellStyle cellStyle) {
        CellUtil.setCellValue(cell, value, cellStyle);
    }

    /**
     * 写入数据行
     */
    private static ExcelWriter writeRow(Map<String, String> headers, List<?> rows, boolean isXlsx) {
        ExcelWriter writer = ExcelUtil.getWriter(isXlsx);
        headers.forEach(writer::addHeaderAlias);
        writer.setOnlyAlias(true);
        writer.write(rows, true);
        return writer;
    }

    /**
     * 合并单元格（ExcelWriter）
     */
    private static void merge(ExcelWriter writer, List<MergeModel> mergeModels) {
        for (MergeModel model : mergeModels) {
            if (model.getCellStyle() == null) {
                writer.merge(model.getFirstRow(), model.getLastRow(), model.getFirstColumn(), 
                    model.getLastColumn(), model.getContent(), model.isSetHeaderStyle());
            } else {
                writer.merge(model.getFirstRow(), model.getLastRow(), model.getFirstColumn(), 
                    model.getLastColumn(), model.getContent(), model.getCellStyle());
            }
        }
    }

    /**
     * 合并单元格（Sheet）
     */
    private static void merge(Sheet sheet, List<MergeModel> mergeModels, CellStyle defaultCellStyle) {
        for (MergeModel model : mergeModels) {
            CellStyle style = model.getCellStyle() == null ? defaultCellStyle : model.getCellStyle();
            CellUtil.mergingCells(sheet, model.getFirstRow(), model.getLastRow(), 
                model.getFirstColumn(), model.getLastColumn(), style);
            
            Row row = sheet.getRow(model.getFirstRow());
            Cell cell = row.getCell(model.getFirstColumn());
            CellUtil.setCellValue(cell, model.getContent(), style);
        }
    }

    /**
     * 刷新输出流
     */
    private static void flush(ExcelWriter writer, OutputStream out) throws IOException {
        try {
            writer.flush(out, true);
        } finally {
            writer.close();
        }
    }

    /**
     * 读取数据
     */
    private static <T> Collection<T> readData(ExcelReader reader, Class<T> targetType,
            int headerRowIndex, int startRowIndex, Map<String, String> targetFields) throws Exception {
        List<T> results = new ArrayList<>();
        List<List<Object>> rows = reader.read();
        
        if (rows == null || rows.isEmpty()) {
            return results;
        }

        // 准备字段映射
        Map<String, Field> fieldMap = prepareFieldMap(targetType, targetFields);
        Map<Integer, Field> headerMap = createHeaderMap(rows.get(headerRowIndex), fieldMap);

        // 读取数据
        for (int i = startRowIndex; i < rows.size(); i++) {
            T data = targetType.getDeclaredConstructor().newInstance();
            List<Object> row = rows.get(i);
            
            for (Map.Entry<Integer, Field> entry : headerMap.entrySet()) {
                Object value = row.get(entry.getKey());
                entry.getValue().set(data, value);
            }
            
            results.add(data);
        }

        return results;
    }

    /**
     * 准备字段映射
     */
    private static Map<String, Field> prepareFieldMap(Class<?> targetType, 
            Map<String, String> targetFields) {
        Map<String, Field> fieldMap = new HashMap<>();
        
        if (targetFields == null || targetFields.isEmpty()) {
            for (Field field : targetType.getDeclaredFields()) {
                field.setAccessible(true);
                fieldMap.put(field.getName(), field);
            }
        } else {
            for (Field field : targetType.getDeclaredFields()) {
                String headerName = targetFields.get(field.getName());
                if (headerName != null) {
                    field.setAccessible(true);
                    fieldMap.put(headerName, field);
                }
            }
        }
        
        return fieldMap;
    }

    /**
     * 创建表头映射
     */
    private static Map<Integer, Field> createHeaderMap(List<Object> headerRow, 
            Map<String, Field> fieldMap) {
        Map<Integer, Field> headerMap = new HashMap<>();
        
        for (int i = 0; i < headerRow.size(); i++) {
            String headerName = headerRow.get(i).toString();
            Field field = fieldMap.get(headerName);
            if (field != null) {
                headerMap.put(i, field);
            }
        }
        
        return headerMap;
    }

    /**
     * 获取或创建单元格样式
     */
    private static CellStyle getOrCreateStyle(Workbook workbook, String key, boolean isHeader) {
        return STYLE_CACHE.computeIfAbsent(key, k -> 
            isHeader ? StyleUtil.createHeadCellStyle(workbook) : StyleUtil.createDefaultCellStyle(workbook)
        );
    }

    /**
     * 验证导出参数
     */
    private static void validateExportParams(Map<String, String> headers, List<?> rows, 
            OutputStream out) {
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("表头映射不能为空");
        }
        if (rows == null) {
            throw new IllegalArgumentException("数据行不能为null");
        }
        if (out == null) {
            throw new IllegalArgumentException("输出流不能为null");
        }
    }

    /**
     * 验证模板参数
     */
    private static void validateTemplateParams(InputStream template, List<String> headers, 
            List<?> rows, OutputStream out) {
        if (template == null) {
            throw new IllegalArgumentException("模板不能为null");
        }
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("表头列表不能为空");
        }
        if (rows == null) {
            throw new IllegalArgumentException("数据行不能为null");
        }
        if (out == null) {
            throw new IllegalArgumentException("输出流不能为null");
        }
    }

    /**
     * 验证导入参数
     */
    private static void validateImportParams(InputStream in, Class<?> targetType) {
        if (in == null) {
            throw new IllegalArgumentException("输入流不能为null");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("目标类型不能为null");
        }
    }

    /**
     * 单元格合并模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MergeModel {
        /** 开始行索引 */
        private int firstRow;
        
        /** 结束行索引 */
        private int lastRow;
        
        /** 开始列索引 */
        private int firstColumn;
        
        /** 结束列索引 */
        private int lastColumn;
        
        /** 合并单元格内容 */
        private Object content;
        
        /** 是否使用表头样式 */
        private boolean isSetHeaderStyle = true;
        
        /** 自定义单元格样式 */
        private CellStyle cellStyle;
    }
}
