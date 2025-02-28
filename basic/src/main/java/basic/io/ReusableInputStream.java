package basic.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 可重复使用的输入流，允许多次读取同一个输入流的内容
 * 
 * 该类将输入流的内容缓存到内存中，允许通过{@link #getInputStream()}方法
 * 重复获取新的输入流来读取相同的内容。每次调用getInputStream都会
 * 返回一个指向开始位置的新输入流。
 *
 * 注意：由于内容被缓存在内存中，请注意输入流的大小，避免出现内存溢出。
 *
 * @author fulin-peng
 * @since 2024/6/8 10:27
 */
public final class ReusableInputStream {

    /** 基础字节数组输入流，用于存储原始数据 */
    private final ByteArrayInputStream basic;

    /**
     * 构造一个可重复使用的输入流
     *
     * @param original 原始输入流
     * @throws IllegalArgumentException 如果输入流为null
     * @throws IOException 如果读取输入流时发生错误
     * @author fulin-peng
     * @since 2024/6/8 10:28
     */
    public ReusableInputStream(InputStream original) throws IOException {
        if (original == null) {
            throw new IllegalArgumentException("输入流不能为null");
        }

        try {
            this.basic = IoUtils.toByteArrayInputStream(original, 0);
        } catch (IOException e) {
            throw new IOException("创建可重用输入流失败", e);
        }
    }

    /**
     * 获取一个新的输入流，指向数据的开始位置
     * 
     * 每次调用此方法都会返回一个新的输入流实例，该实例可以独立读取
     * 原始输入流的全部内容。这个方法可以被多次调用，每次都会得到
     * 一个从头开始的新输入流。
     *
     * @return InputStream 新的输入流实例
     * @author fulin-peng
     * @since 2024/6/8 10:29
     */
    public InputStream getInputStream() {
        basic.reset();
        return basic;
    }

    /**
     * 获取底层字节数组的大小
     *
     * @return 字节数组的长度
     */
    public int size() {
        return basic.available();
    }
}
