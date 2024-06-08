package basic.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * I/O工具类
 *
 * @author fulin-peng
 * 2024-06-08  10:33
 */
public class IoUtils {

    /**
     * 输入流转字节数组输出流
     * @param input 输入流
     * @param bufferSize 读取字节单位大小，默认1024
     * 2024/6/8 0008 10:35
     * @author fulin-peng
     */
    public static ByteArrayOutputStream inToByteArrayOutStream(InputStream input,int bufferSize)throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize<=0?1024:bufferSize];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        return byteArrayOutputStream;
    }

    /**
     * 输入流转字节数组输入流
     * @param input 输入流
     * @param bufferSize 读取字节单位大小，默认1024
     * 2024/6/8 0008 10:44
     * @author fulin-peng
     */
    public static ByteArrayInputStream inToByteArrayInStream(InputStream input, int bufferSize)throws IOException {
        return new ByteArrayInputStream(inToByteArrayOutStream(input, bufferSize).toByteArray());
    }

    /**
     * 输入流转字节数组
     * @param input 字节流
     * 2024/6/8 0008 10:34
     * @author fulin-peng
     */
    private static byte[] toByteArray(InputStream input) throws IOException {
        return inToByteArrayOutStream(input, 0).toByteArray();
    }
}
