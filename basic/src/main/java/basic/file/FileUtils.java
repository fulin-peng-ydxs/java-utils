package basic.file;


import basic.collection.CollectionUtils;
import basic.string.StringUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件工具
 * author: pengshuaifeng
 * 2023/8/31
 */
public class FileUtils {

    /**
     *写出文件
     *2023/9/2 09:14
     *@author pengshuaifeng
     *@param content 字节数组
     *@param fileName 文件名
     *@param path 文件目录
     */
    public static void write(byte[] content,String fileName,String path){
        try(OutputStream outputStream =  getOutputStream(fileName,path)){
            outputStream.write(content);
        } catch (Exception e) {
           throw new RuntimeException("文件输出异常：",e);
        }
    }

    /**
     * 获取文件输出流
     * 2023/12/30 13:32
     * @author pengshuaifeng
     * @param fileName 文件名
     * @param path 文件目录
     */
    public static OutputStream getOutputStream(String fileName,String path){
        try {
            return new FileOutputStream(createFile(path==null?getSystemHomePath():path, fileName));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("获取文件输出流异常：",e);
        }
    }

    /**
     * 获取系统当前用户目录路径
     * 2023/12/30 13:37
     * @author pengshuaifeng
     */
    public static String getSystemHomePath(){
        File homeDirectory = FileSystemView.getFileSystemView() .getHomeDirectory();
        return homeDirectory.getAbsolutePath();
    }

    /**
     * 路径拼接
     * 2024/8/8 下午3:32
     * @author fulin-peng
     */
    public static String pathSeparator(String rootPath,String path){
        String separator = File.separator;
        if(rootPath.endsWith(separator) && path.startsWith(separator))
            return rootPath+ StringUtils.substring(path,separator,null,false,true);
        else if(!rootPath.endsWith(separator) && !path.startsWith(separator))
            return rootPath + File.separator + path;
        else return rootPath+path;
    }

    /**
     * 创建文件
     * 2023/9/8 22:34
     * @author pengshuaifeng
     */
    public static File createFile(String mkdirPath,String fileName){
        File mkdir = new File(mkdirPath);
        if(!mkdir.exists()){
            try {
                mkdir.mkdirs();
            } catch (Exception e) {
                throw new RuntimeException("文件目录创建异常："+mkdirPath,e);
            }
        }
        String filePath = mkdir + File.separator + fileName;
        File file = new File(filePath);
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (Exception e) {
                throw new RuntimeException("文件创建失败："+filePath,e);
            }
        }
        return file;
    }


    /**文件转字符串集合
     * 2022/9/26 0026-15:43
     * @param reader 读取字符流
     * @param isClearSpacing 是否清除空格
     * @param isClearBlankLines 是否清除空行
     * @param codeBr 换行符
     * @author pengfulin
     */
    public static List<String> fileToLines(Reader reader, boolean isClearSpacing , boolean isClearBlankLines, String codeBr) {
        try {
            LinkedList<String> lines = new LinkedList<>();
            BufferedReader bufferedReader = new BufferedReader(reader);
            String lineTemp;
            while ((lineTemp = bufferedReader.readLine()) != null) {
                StringBuilder builderLine = new StringBuilder();
                if (isClearSpacing)
                    lineTemp = lineTemp.trim();
                if(isClearBlankLines){
                    if (!(lineTemp.trim().length()>0))
                        continue; //去除空行
                }
                builderLine.append(lineTemp);
                if(codeBr!=null)
                    builderLine.append(codeBr);
                lines.add(builderLine.toString());
            }
            return lines.isEmpty()?null:lines;
        } catch (Exception e) {
            throw new RuntimeException("文件转化字符串集合异常",e);
        }
    }

    /**文件转字符串
     * 2022/10/10 0010-14:30
     * @param reader 读取字符流
     * @param isClearSpacing 是否清除空格
     * @param isClearBlankLines 是否清除空行
     * @param codeBr 换行符
     * @author pengfulin
     */
    public static String fileToString(Reader reader, boolean isClearSpacing , boolean isClearBlankLines, String codeBr){
        List<String> fileToLines = fileToLines(reader, isClearSpacing, isClearBlankLines, codeBr);
        if(CollectionUtils.isEmpty(fileToLines))
            return null;
        StringBuilder builder = new StringBuilder();
        for (String fileToLine : fileToLines)
            builder.append(fileToLine);
        return builder.length()==0?null:builder.toString();
    }

    public static String fileToString(Reader reader){
        return fileToString(reader, false, false, null);
    }

    public static String fileToString(Reader reader,String codeBr){
        return fileToString(reader, false, false, codeBr);
    }
}
