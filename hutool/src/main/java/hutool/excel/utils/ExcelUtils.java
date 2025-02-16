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

/**
 * excel工具类
 *
 * @author pengshuaifeng
 * 2023/12/30
 */
public class ExcelUtils {


    /**导出数据
     * 2022/11/23 0023-16:31
     * @author pengfulin
     * @param headers 表头：中文名：属性名
     * @param rows 导出的数据集合
     * @param out 导出流
     * @param isXlsx 是否导出为xlsx格式
     * @param mergeModels 合并单元格模型
     */
    //TODO 默认只写入第一个sheet，暂不支持普通数据单元格样式处理&自定义样式配置
    public static void export(Map<String,String> headers, List<?> rows,OutputStream out, boolean isXlsx, List<MergeModel> mergeModels){
        try {
            ExcelWriter writer = writeRow(headers, rows, isXlsx);
            if(mergeModels!=null){
                merge(writer,mergeModels);
            }
            flush(writer,out);
        } catch (IOException e) {
            throw new RuntimeException("excel导出异常",e);
        }
    }

    /**导出数据
     * 2022/11/24 0024-10:46
     * @author pengfulin
     * @param ignoreFields 需忽略的的实体属性
     */
    public static void exportNoHead(Set<String> ignoreFields, List<?> rows,OutputStream out, boolean isXlsx, List<MergeModel> mergeModels){
        export(defineHeaders(rows.get(0).getClass(),ignoreFields),rows,out,isXlsx,mergeModels);
    }


    /**导出数据：使用模板导出
     * 2022/11/25 0025-15:36
     * @author pengfulin
     * @param templatePath 模板路径：使用类路径
     */
    public static void exportByModel(String templatePath,List<String> headers,OutputStream out,List<?> rows,
                                         int startRowIndex,int startColumnIndex, List<MergeModel> mergeModels){
        exportByModel(Objects.requireNonNull(ExcelUtils.class.getResourceAsStream(templatePath)),headers,out,rows,startRowIndex,
                startColumnIndex,mergeModels);
    }

    /**
     * 导出数据：使用模版导出
     * 2023/12/30 12:20
     * @param in 模板流
     * @param headers 表头：实体属性，将按照顺序一一对应各列
     * @param startRowIndex 起始行索引：从0开始
     * @param startColumnIndex 起始列索引：从0开始
     * @param out 导出流
     * @param rows 导出的数据集合
     * @param mergeModels 合并单元格模型
     * @author pengshuaifeng
     */
    //TODO 存在模板的样式背景问题，即会移除掉模版背景样式
    public static void exportByModel(InputStream in, List<String> headers,OutputStream out, List<?> rows,
                                     int startRowIndex, int startColumnIndex, List<MergeModel> mergeModels){
        try {
            //获取模版
            Workbook workbook = WorkbookFactory.create(in);
            Sheet sheet = workbook.getSheetAt(0); //TODO 暂时只处理第一个sheet
            //定义样式
            CellStyle mergeCellStyle = StyleUtil.createHeadCellStyle(workbook); //合并单元格样式：取表头样式
            CellStyle defaultCellStyle =StyleUtil.createDefaultCellStyle(workbook);//数据单元格样式：取默认样式
            //定义数据起始行列
            int startRow=Math.max(1,startRowIndex);
            int startColumn=Math.max(0,startColumnIndex);
            //填充数据
            for (Object row : rows) {
                Row tempRow = sheet.createRow(startRow);
                for (String header : headers) {
                    Cell cell = tempRow.createCell(startColumn);
                    writeCellValue(cell,ClassUtils.getFieldValue(header,row,row.getClass()) ,defaultCellStyle);
                    startColumn++;
                }
                startColumn=Math.max(0,startColumnIndex);
                startRow++;
            }
            //合并单元格
            if(mergeModels!=null){
                merge(sheet,mergeModels,mergeCellStyle);
            }
            //刷新数据
            flush(workbook,out);
        } catch (Exception e) {
            throw new RuntimeException("excel模版导出异常",e);
        }
    }

    /**解析表头
     * 2022/11/24 0024-10:10
     * @author pengfulin
     * @param type 属性实体类型
     * @param ignoreFields 需忽略的的实体属性
     */
    public static Map<String,String> defineHeaders(Class<?> type, Collection<String> ignoreFields){
        Map<String, String> fields = new LinkedHashMap<>();
        Field[] declaredFields = type.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            String name = declaredField.getName();
            if(ignoreFields!=null&&ignoreFields.contains(name))
                continue;
            //TODO 可根据实际情况自主定义获取字段中文的注解，一般建议使用swagger相关
            fields.put(name,name);
        }
        return fields;
    }


    /**写入数据：向单元格写入值
     * 2022/11/25 0025-17:50
     * @author pengfulin
     * @param cell 单元格对象
     * @param value 值对象
     * @param cellStyle 单元格样式
     */
    private static void writeCellValue(Cell cell,Object value,CellStyle cellStyle){
        CellUtil.setCellValue(cell,value,cellStyle);
    }

    /**写入数据：向单元行写入值
     * 2022/11/23 0023-18:28
     * @author pengfulin
     */
    private static ExcelWriter writeRow(Map<String,String> headers,List<?> rows,boolean isXlsx){
        ExcelWriter writer = ExcelUtil.getWriter(isXlsx);
        headers.forEach(writer::addHeaderAlias);  //写入表头
        writer.setOnlyAlias(true); //忽略无别名的列
        writer.write(rows, true); //写入行
        return writer;
    }


    /**刷出数据
     * 2022/11/23 0023-18:28
     * @author pengfulin
     */
    private static void flush(ExcelWriter writer,OutputStream out) throws IOException {
        try {
            writer.flush(out, true);
        } finally {
            writer.close();
        }
    }
    /**刷出数据
     * 2022/11/25 0025-19:05
     * @author pengfulin
     */
    private static void flush(Workbook workbook,OutputStream out)  throws IOException {
        try {
            workbook.write(out);
            out.flush();
        } finally {
            IoUtil.close(out);
            workbook.close();
        }
    }

    /**
     * 导入数据
     * <p>默认导入第一个sheet，第一行为表头，第二行第一列开始读取数据</p>
     * @param in excel流
     * @param targetType excel构建对象类型
     * 2024/1/19 01:18
     * @author pengshuaifeng
     */
    //TODO 暂只支持读取一个sheet
    public static  <T> Collection<T> read(InputStream in,Class<T> targetType) throws Exception {
        return read(in,targetType,0,0,1,null);
    }

    /**
     * 导入数据
     * <p>默认第一行为表头，第二行第一列开始读取数据</p>
     * @param in excel流
     * @param targetType excel构建对象类型
     * @param sheetName excel导入的sheet名
     * 2024/1/19 01:18
     * @author pengshuaifeng
     */
    public static  <T> Collection<T> read(InputStream in,Class<T> targetType,String sheetName) throws Exception {
        return read(in,targetType,sheetName,0,1,null);
    }

    /**
     * 导入数据
     * <p>默认导入第一个sheet，第一行为表头，第二行第一列开始读取数据</p>
     * @param in excel流
     * @param targetType excel构建对象类型
     * @param targetFields 构建对象字段的表头映射：key为字段名 ，value为表头列名，没有则根据默认规则生成进行匹配
     * 2024/1/19 01:18
     * @author pengshuaifeng
     */
    public static  <T> Collection<T> read(InputStream in,Class<T> targetType,Map<String,String> targetFields) throws Exception {
        return read(in,targetType,0,0,1,targetFields);
    }

    /**
     * 导入数据
     * <p>默认第一行为表头，第二行第一列开始读取数据</p>
     * @param in excel流
     * @param targetType excel构建对象类型
     * @param sheetName excel导入的sheet名
     * @param targetFields 构建对象字段的表头映射：key为字段名 ，value为表头列名，没有则根据默认规则生成进行匹配
     * 2024/1/19 01:18
     * @author pengshuaifeng
     */
    public static  <T> Collection<T> read(InputStream in,Class<T> targetType,String sheetName,Map<String,String> targetFields) throws Exception {
        return read(in,targetType,sheetName,0,1,targetFields);
    }

    /**
     * 导入数据
     * @param in excel流
     * @param targetType excel构建对象类型
     * @param sheetIndex excel导入的sheet索引
     * @param headerIndex 表头所在行的索引
     * @param startRowIndex 数据所在的开始行的索引
     * @param targetFields 构建对象字段的表头映射：key为字段名 ，value为表头列名，没有则根据默认规则生成进行匹配
     * 2024/1/19 01:18
     * @author pengshuaifeng
     */
    public static  <T> Collection<T> read(InputStream in,Class<T> targetType,int sheetIndex,int headerIndex,int startRowIndex,Map<String,String> targetFields) throws Exception {
        //加载数据
        ExcelReader reader = ExcelUtil.getReader(in, sheetIndex);
        return read(reader,targetType,headerIndex,startRowIndex,targetFields);
    }

    /**
     * 导入数据
     * @param in excel流
     * @param targetType excel构建对象类型
     * @param sheetName excel导入的sheet名
     * @param headerIndex 表头所在行的索引
     * @param startRowIndex 数据所在的开始行的索引
     * @param targetFields 构建对象字段的表头映射：key为字段名 ，value为表头列名，没有则根据默认规则生成进行匹配
     * 2024/1/19 01:18
     * @author pengshuaifeng
     */
    public static  <T> Collection<T> read(InputStream in,Class<T> targetType,String sheetName,int headerIndex,int startRowIndex,Map<String,String> targetFields) throws Exception{
        //加载数据
        ExcelReader reader = ExcelUtil.getReader(in, sheetName);
        return read(reader,targetType,headerIndex,startRowIndex,targetFields);
    }

    /**
     * 导入数据
     * @param reader excel读取器
     * @param targetType excel构建对象类型
     * @param headerIndex 表头所在行的索引
     * @param startRowIndex 数据所在的开始行的索引
     * @param targetFieldNames 构建对象字段的表头映射：key为字段名 ，value为表头列名，没有则根据默认规则生成进行匹配
     * 2024/1/19 01:18
     * @author pengshuaifeng
     */
    private static  <T> Collection<T> read(ExcelReader reader,Class<T> targetType,int headerIndex,int startRowIndex,Map<String,String> targetFieldNames) throws Exception{
        List<T> results = new LinkedList<>();  //结果集
        //加载数据行
        List<List<Object>> rows = reader.read();
        if(rows!=null &&!rows.isEmpty()){
            Map<String,Field> targetFields = new HashMap<>();
            //获取表头映射
            if(targetFieldNames==null){
                for (Field declaredField : targetType.getDeclaredFields()) {
                    //TODO 后续通过属性注解实现中文名映射，建议使用swagger
                    declaredField.setAccessible(true);
                    targetFields.put(declaredField.getName(),declaredField);
                }
            }else{
                for (Field declaredField : targetType.getDeclaredFields()) {
                    String headerName = targetFieldNames.get(declaredField.getName());
                    if (headerName!=null) {
                        declaredField.setAccessible(true);
                        targetFields.put(headerName,declaredField);
                    }
                }
            }
            Map<Integer,Field> headerMappings=new HashMap<>();
            List<Object> headerRow = rows.get(headerIndex);
            for (int i = 0; i < headerRow.size(); i++) {
                Field field = targetFields.get(headerRow.get(i).toString());
                if(field==null)
                    continue;
                headerMappings.put(i,field);
            }
            //读取数据
            for (int rowIndex = startRowIndex; rowIndex < rows.size(); rowIndex++) {  //遍历每一行
                T data = targetType.newInstance();
                List<Object> row = rows.get(rowIndex);
                for (Map.Entry<Integer, Field> fieldEntry : headerMappings.entrySet()) {
                    Object colData = row.get(fieldEntry.getKey());
                    fieldEntry.getValue().set(data,colData); //将关联的列的值赋值到字段中
                }
                results.add(data);
            }
        }
        return results;
    }


    /**合并单元格
     * 2022/11/25 0025-17:56
     * @author pengfulin
     */
    private static void merge(Sheet sheet,List<MergeModel> mergeModels,CellStyle defaultCellStyle){
        mergeModels.forEach(value->{
            value.setCellStyle(value.getCellStyle()==null?defaultCellStyle:value.getCellStyle());
            //合并单元格
            CellUtil.mergingCells(sheet, value.getFirstRow(), value.getLastRow(), value.firstColumn, value.getLastColumn(),
                    value.getCellStyle());
            //设置内容值
            Row row = sheet.getRow(value.getFirstRow());
            Cell cell = row.getCell(value.getFirstColumn());
            CellUtil.setCellValue(cell, value.getContent(), value.getCellStyle());
        });
    }

    /**合并单元格
     * 2022/11/24 0024-10:50
     * @author pengfulin
     */
    private static void merge(ExcelWriter writer,List<MergeModel> mergeModels){
        mergeModels.forEach(value->{
            if(value.cellStyle==null){
                writer.merge(value.firstRow, value.lastRow, value.firstColumn, value.lastColumn,value.content,value.isSetHeaderStyle);
            }else{
                writer.merge(value.firstRow, value.lastRow, value.firstColumn, value.lastColumn,value.content,value.cellStyle);
            }
        });
    }

    /** 单元格合并模型*/
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MergeModel{
        private int firstRow; //开始行
        private int lastRow; //结束行
        private int firstColumn; //开始列
        private int lastColumn; //结束列
        private Object content; //合并内容
        private boolean isSetHeaderStyle=true;  //是否为标题头样式
        private CellStyle cellStyle; //使用自定义样式
    }
}
