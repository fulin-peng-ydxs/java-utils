package basic.file;


import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

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

}
