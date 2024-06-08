package basic.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 可重复输入流
 * 2024/6/8 0008 10:27
 * @author fulin-peng
 */
public class ReusableInputStream {

    private final ByteArrayInputStream basic;

    /**
     * 构造函数
     * @param original 原始流数据
     * 2024/6/8 0008 10:28
     * @author fulin-peng
     */
    public ReusableInputStream(InputStream original) throws IOException {
        this.basic = IoUtils.inToByteArrayInStream(original,0);
    }

    /**
     * 获取一个新的输入流
     * 2024/6/8 0008 10:29
     * @author fulin-peng
     */
    public InputStream getInputStream() {
        basic.reset();
        return basic;
    }
}
