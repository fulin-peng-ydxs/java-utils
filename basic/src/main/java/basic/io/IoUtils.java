package basic.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * I/O工具类，提供输入输出流操作、字节数组转换等功能
 *
 * @author fulin-peng
 * @since 2024-06-08
 */
public class IoUtils {

    private IoUtils() {
        // Utility class should not be instantiated
    }

    /** 缓冲区大小常量 */
    private static final int DEFAULT_BUFFER_SIZE = 8192; // 增加默认缓冲区大小以提高性能
    private static final int MIN_BUFFER_SIZE = 1024;    // 增加最小缓冲区大小
    private static final int MAX_BUFFER_SIZE = 32768;   // 增加最大缓冲区大小

    /** 默认字符集 */
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /** 错误消息 */
    private static final String NULL_INPUT = "输入流不能为null";
    private static final String NULL_OUTPUT = "输出流不能为null";
    private static final String NULL_READER = "Reader不能为null";
    private static final String NULL_WRITER = "Writer不能为null";
    private static final String NULL_FILE = "文件不能为null";
    private static final String READ_ERROR = "读取输入流时发生错误";
    private static final String WRITE_ERROR = "写入输出流时发生错误";
    private static final String CLOSE_ERROR = "关闭资源时发生错误";
    private static final String FILE_NOT_FOUND = "文件不存在";
    private static final String FILE_READ_ERROR = "读取文件时发生错误";
    private static final String FILE_WRITE_ERROR = "写入文件时发生错误";

    /** 线程池配置 */
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int QUEUE_CAPACITY = 1000;

    /** 异步操作线程池 */
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAX_POOL_SIZE,
        KEEP_ALIVE_TIME,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(QUEUE_CAPACITY),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 将输入流转换为字节数组输出流
     * 
     * @param input 输入流
     * @param bufferSize 读取缓冲区大小，如果小于等于0则使用默认值1024
     * @return ByteArrayOutputStream 包含输入流内容的字节数组输出流
     * @throws IllegalArgumentException 如果输入流为null
     * @throws IOException 如果读取过程中发生I/O错误
     * @author fulin-peng
     * @since 2024/6/8
     */
    public static ByteArrayOutputStream toByteArrayOutputStream(InputStream input, int bufferSize) 
            throws IOException {
        if (input == null) {
            throw new IllegalArgumentException(NULL_INPUT);
        }

        int actualBufferSize = normalizeBufferSize(bufferSize);
        ByteArrayOutputStream output = new ByteArrayOutputStream(actualBufferSize);
        
        try {
            if (input instanceof FileInputStream) {
                // 对于文件输入流，使用channel方式提高性能
                FileChannel channel = ((FileInputStream) input).getChannel();
                ByteBuffer buffer = ByteBuffer.allocateDirect(actualBufferSize);
                while (channel.read(buffer) != -1) {
                    buffer.flip();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    output.write(bytes);
                    buffer.clear();
                }
            } else {
                copy(input, output, actualBufferSize);
            }
            return output;
        } catch (IOException e) {
            closeQuietly(output);
            throw new IOException(READ_ERROR, e);
        }
    }

    /**
     * 将输入流转换为字节数组输入流
     * 
     * @param input 输入流
     * @param bufferSize 读取缓冲区大小，如果小于等于0则使用默认值1024
     * @return ByteArrayInputStream 包含输入流内容的字节数组输入流
     * @throws IllegalArgumentException 如果输入流为null
     * @throws IOException 如果读取过程中发生I/O错误
     * @author fulin-peng
     * @since 2024/6/8
     */
    public static ByteArrayInputStream toByteArrayInputStream(InputStream input, int bufferSize) 
            throws IOException {
        if (input == null) {
            throw new IllegalArgumentException(NULL_INPUT);
        }

        try (ByteArrayOutputStream output = toByteArrayOutputStream(input, bufferSize)) {
            return new ByteArrayInputStream(output.toByteArray());
        }
    }

    public static ByteArrayInputStream toByteArrayInputStream(InputStream input)
            throws IOException {
        return toByteArrayInputStream(input, 0);
    }

    /**
     * 将字节数组转换为输入流
     *
     * @param input 字节数组
     * @return ByteArrayInputStream 包含字节数组内容的输入流
     * @throws IllegalArgumentException 如果字节数组为null
     */
    public static ByteArrayInputStream toByteArrayInputStream(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("字节数组不能为null");
        }
        return new ByteArrayInputStream(input);
    }

    /**
     * 将输入流转换为字节数组
     * 
     * @param input 输入流
     * @return byte[] 包含输入流内容的字节数组
     * @throws IllegalArgumentException 如果输入流为null
     * @throws IOException 如果读取过程中发生I/O错误
     * @author fulin-peng
     * @since 2024/6/8
     */
    public static byte[] toByteArray(InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException(NULL_INPUT);
        }

        try (ByteArrayOutputStream output = toByteArrayOutputStream(input, DEFAULT_BUFFER_SIZE)) {
            return output.toByteArray();
        }
    }

    /**
     * 将字符串转换为输入流
     * 
     * @param input 输入字符串
     * @param charset 字符集，为null时使用UTF-8
     * @return 包含字符串内容的输入流
     * @throws IllegalArgumentException 如果输入字符串为null
     */
    public static InputStream toInputStream(String input, Charset charset) {
        if (input == null) {
            throw new IllegalArgumentException("输入字符串不能为null");
        }
        return new ByteArrayInputStream(input.getBytes(charset != null ? charset : DEFAULT_CHARSET));
    }

    /**
     * 将输入流转换为字符串
     * 
     * @param input 输入流
     * @param charset 字符集，为null时使用UTF-8
     * @return 输入流内容的字符串表示
     * @throws IllegalArgumentException 如果输入流为null
     * @throws IOException 如果读取过程中发生I/O错误
     */
    public static String toString(InputStream input, Charset charset) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException(NULL_INPUT);
        }
        return new String(toByteArray(input), charset != null ? charset : DEFAULT_CHARSET);
    }

    /**
     * 复制输入流到输出流
     * 
     * @param input 输入流
     * @param output 输出流
     * @param bufferSize 缓冲区大小
     * @return 复制的字节数
     * @throws IllegalArgumentException 如果任一参数为null
     * @throws IOException 如果复制过程中发生I/O错误
     */
    public static long copy(InputStream input, OutputStream output, int bufferSize) 
            throws IOException {
        if (input == null) {
            throw new IllegalArgumentException(NULL_INPUT);
        }
        if (output == null) {
            throw new IllegalArgumentException(NULL_OUTPUT);
        }

        int actualBufferSize = normalizeBufferSize(bufferSize);
        
        // 对于文件流，使用NIO Channel方式复制
        if (input instanceof FileInputStream && output instanceof FileOutputStream) {
            try (FileChannel sourceChannel = ((FileInputStream) input).getChannel();
                 FileChannel targetChannel = ((FileOutputStream) output).getChannel()) {
                return sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
            }
        }
        
        // 对于其他类型的流，使用缓冲区复制
        byte[] buffer = new byte[actualBufferSize];
        long count = 0;
        int n;

        if (!(input instanceof BufferedInputStream)) {
            input = new BufferedInputStream(input, actualBufferSize);
        }
        if (!(output instanceof BufferedOutputStream)) {
            output = new BufferedOutputStream(output, actualBufferSize);
        }

        try {
            while ((n = input.read(buffer)) != -1) {
                output.write(buffer, 0, n);
                count += n;
            }
            output.flush();
            return count;
        } catch (IOException e) {
            throw new IOException(WRITE_ERROR, e);
        }
    }

    /**
     * 复制Reader到Writer
     * 
     * @param reader Reader
     * @param writer Writer
     * @param bufferSize 缓冲区大小
     * @return 复制的字符数
     * @throws IllegalArgumentException 如果任一参数为null
     * @throws IOException 如果复制过程中发生I/O错误
     */
    public static long copy(Reader reader, Writer writer, int bufferSize) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException(NULL_READER);
        }
        if (writer == null) {
            throw new IllegalArgumentException(NULL_WRITER);
        }

        int actualBufferSize = normalizeBufferSize(bufferSize);
        char[] buffer = new char[actualBufferSize];
        long count = 0;
        int n;

        if (!(reader instanceof BufferedReader)) {
            reader = new BufferedReader(reader, actualBufferSize);
        }
        if (!(writer instanceof BufferedWriter)) {
            writer = new BufferedWriter(writer, actualBufferSize);
        }

        while ((n = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, n);
            count += n;
        }
        writer.flush();
        return count;
    }

    /**
     * 使用默认缓冲区大小复制输入流到输出流
     * 
     * @param input 输入流
     * @param output 输出流
     * @return 复制的字节数
     * @throws IllegalArgumentException 如果任一参数为null
     * @throws IOException 如果复制过程中发生I/O错误
     */
    public static long copy(InputStream input, OutputStream output) throws IOException {
        return copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 使用默认缓冲区大小复制Reader到Writer
     * 
     * @param reader Reader
     * @param writer Writer
     * @return 复制的字符数
     * @throws IllegalArgumentException 如果任一参数为null
     * @throws IOException 如果复制过程中发生I/O错误
     */
    public static long copy(Reader reader, Writer writer) throws IOException {
        return copy(reader, writer, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 安全关闭Closeable资源
     * 
     * @param closeable 待关闭的资源
     */
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            // 记录关闭时的异常，但不抛出
            System.err.println(CLOSE_ERROR + ": " + e.getMessage());
        }
    }

    /**
     * 从文件读取字符串
     * @param file 文件
     * @param charset 字符集，为null时使用UTF-8
     * @return 文件内容
     * @throws IOException 如果读取过程中发生错误
     */
    public static String readFileToString(File file, Charset charset) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        if (!file.exists()) {
            throw new FileNotFoundException(FILE_NOT_FOUND);
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            return toString(fis, charset);
        } catch (IOException e) {
            throw new IOException(FILE_READ_ERROR, e);
        }
    }

    /**
     * 将字符串写入文件
     * @param file 文件
     * @param data 要写入的数据
     * @param charset 字符集，为null时使用UTF-8
     * @param append 是否追加模式
     * @throws IOException 如果写入过程中发生错误
     */
    public static void writeStringToFile(File file, String data, Charset charset, boolean append) 
            throws IOException {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        if (data == null) {
            throw new IllegalArgumentException("数据不能为null");
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建目录: " + parent);
        }

        try (FileOutputStream fos = new FileOutputStream(file, append)) {
            fos.write(data.getBytes(charset != null ? charset : DEFAULT_CHARSET));
        } catch (IOException e) {
            throw new IOException(FILE_WRITE_ERROR, e);
        }
    }

    /**
     * 将输入流写入文件
     * @param file 目标文件
     * @param input 输入流
     * @param append 是否追加模式
     * @throws IOException 如果写入过程中发生错误
     */
    public static void copyInputStreamToFile(File file, InputStream input, boolean append) 
            throws IOException {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        if (input == null) {
            throw new IllegalArgumentException(NULL_INPUT);
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建目录: " + parent);
        }

        try (FileOutputStream output = new FileOutputStream(file, append)) {
            copy(input, output);
        } catch (IOException e) {
            throw new IOException(FILE_WRITE_ERROR, e);
        }
    }

    /**
     * 异步读取文件
     * @param file 文件
     * @param charset 字符集，为null时使用UTF-8
     * @param callback 回调函数，用于处理读取结果
     */
    public static void readFileAsync(File file, Charset charset, Consumer<String> callback) {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        if (callback == null) {
            throw new IllegalArgumentException("回调函数不能为null");
        }

        EXECUTOR.submit(() -> {
            try {
                String content = readFileToString(file, charset);
                callback.accept(content);
            } catch (IOException e) {
                System.err.println(FILE_READ_ERROR + ": " + e.getMessage());
            }
        });
    }

    /**
     * 异步写入文件
     * @param file 文件
     * @param data 要写入的数据
     * @param charset 字符集，为null时使用UTF-8
     * @param append 是否追加模式
     * @param callback 回调函数，用于处理写入完成事件
     */
    public static void writeFileAsync(File file, String data, Charset charset, boolean append, 
            Runnable callback) {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        if (data == null) {
            throw new IllegalArgumentException("数据不能为null");
        }

        EXECUTOR.submit(() -> {
            try {
                writeStringToFile(file, data, charset, append);
                if (callback != null) {
                    callback.run();
                }
            } catch (IOException e) {
                System.err.println(FILE_WRITE_ERROR + ": " + e.getMessage());
            }
        });
    }

    /**
     * 使用NIO异步通道读取文件
     * @param file 文件
     * @param charset 字符集，为null时使用UTF-8
     * @return Future对象，包含文件内容
     */
    public static Future<String> readFileWithAsyncChannel(File file, Charset charset) {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        Path path = file.toPath();

        try {
            AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                path, StandardOpenOption.READ);
            ByteBuffer buffer = ByteBuffer.allocate((int) file.length());

            channel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    try {
                        attachment.flip();
                        byte[] bytes = new byte[attachment.remaining()];
                        attachment.get(bytes);
                        String content = new String(bytes, 
                            charset != null ? charset : DEFAULT_CHARSET);
                        future.complete(content);
                        channel.close();
                    } catch (IOException e) {
                        future.completeExceptionally(e);
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    future.completeExceptionally(exc);
                    try {
                        channel.close();
                    } catch (IOException e) {
                        // 忽略关闭时的异常
                    }
                }
            });
        } catch (IOException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * 使用NIO异步通道写入文件
     * @param file 文件
     * @param data 要写入的数据
     * @param charset 字符集，为null时使用UTF-8
     * @param append 是否追加模式
     * @return Future对象，写入完成时返回true
     */
    public static Future<Boolean> writeFileWithAsyncChannel(File file, String data, 
            Charset charset, boolean append) {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        if (data == null) {
            throw new IllegalArgumentException("数据不能为null");
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Path path = file.toPath();

        try {
            Set<StandardOpenOption> options = new HashSet<>();
            options.add(StandardOpenOption.WRITE);
            options.add(StandardOpenOption.CREATE);
            if (append) {
                options.add(StandardOpenOption.APPEND);
            } else {
                options.add(StandardOpenOption.TRUNCATE_EXISTING);
            }

            AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, 
                options.toArray(new StandardOpenOption[0]));
            byte[] bytes = data.getBytes(charset != null ? charset : DEFAULT_CHARSET);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);

            channel.write(buffer, append ? file.length() : 0, buffer, 
                new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        try {
                            channel.close();
                            future.complete(true);
                        } catch (IOException e) {
                            future.completeExceptionally(e);
                        }
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        future.completeExceptionally(exc);
                        try {
                            channel.close();
                        } catch (IOException e) {
                            // 忽略关闭时的异常
                        }
                    }
                });
        } catch (IOException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * 安全关闭多个Closeable资源
     * 
     * @param closeables 待关闭的资源数组
     */
    public static void closeQuietly(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                closeQuietly(closeable);
            }
        }
    }

    /**
     * 规范化缓冲区大小
     * 
     * @param bufferSize 请求的缓冲区大小
     * @return 规范化后的缓冲区大小
     */
    private static int normalizeBufferSize(int bufferSize) {
        if (bufferSize <= 0) {
            return DEFAULT_BUFFER_SIZE;
        }
        if (bufferSize < MIN_BUFFER_SIZE) {
            return MIN_BUFFER_SIZE;
        }
        if (bufferSize > MAX_BUFFER_SIZE) {
            return MAX_BUFFER_SIZE;
        }
        return bufferSize;
    }

    /**
     * 关闭线程池
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
