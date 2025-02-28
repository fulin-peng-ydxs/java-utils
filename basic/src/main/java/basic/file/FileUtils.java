package basic.file;

import basic.collection.CollectionUtils;
import basic.io.IoUtils;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.*;

/**
 * 文件操作工具类，提供文件读写、路径处理、目录操作等功能
 * 该类与IoUtils协同工作，专注于文件系统操作，而将底层IO操作委托给IoUtils
 *
 * @author pengshuaifeng
 * @since 2023/8/31
 */
public class FileUtils {

    private FileUtils() {
        // Utility class should not be instantiated
    }

    /** 常量 */
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int DEFAULT_BUILDER_CAPACITY = 1024;
    private static final int DEFAULT_WATCH_INTERVAL = 5000; // 默认监控间隔（毫秒）
    private static final int COMPRESSION_LEVEL = 9; // 最大压缩级别
    private static final int MAX_PATH_LENGTH = 255; // 最大路径长度

    /** 默认字符集 */
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /** 文件大小单位 */
    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;
    private static final long TB = GB * 1024;

    /** 错误消息 */
    private static final String NULL_CONTENT = "文件内容不能为null";
    private static final String NULL_FILENAME = "文件名不能为空";
    private static final String NULL_PATH = "路径不能为null";
    private static final String NULL_FILE = "文件不能为null";
    private static final String NULL_READER = "Reader不能为null";
    private static final String CREATE_DIR_ERROR = "创建目录失败: ";
    private static final String CREATE_FILE_ERROR = "创建文件失败: ";
    private static final String WRITE_ERROR = "文件写入失败: ";
    private static final String READ_ERROR = "文件读取失败: ";
    private static final String COPY_ERROR = "文件复制失败: ";
    private static final String MOVE_ERROR = "文件移动失败: ";
    private static final String DELETE_ERROR = "文件删除失败: ";
    private static final String HASH_ERROR = "计算文件哈希值失败: ";
    private static final String ZIP_ERROR = "压缩文件失败: ";
    private static final String UNZIP_ERROR = "解压文件失败: ";
    private static final String PATH_TOO_LONG = "路径长度超过系统限制: ";
    private static final String INVALID_PATH_CHARS = "路径包含非法字符: ";

    /** 哈希算法 */
    private static final String MD5 = "MD5";
    private static final String SHA1 = "SHA-1";
    private static final String SHA256 = "SHA-256";

    /** 系统特定的路径分隔符 */
    private static final String PATH_SEPARATOR = File.separator;
    private static final String WINDOWS_SEPARATOR = "\\";
    private static final String UNIX_SEPARATOR = "/";

    /** 非法路径字符（Windows） */
    private static final Set<Character> INVALID_PATH_CHARS_WINDOWS = new HashSet<>(
        Arrays.asList('\\', '/', ':', '*', '?', '"', '<', '>', '|')
    );

    /**
     * 计算文件的MD5值
     * @param file 文件
     * @return MD5哈希值的十六进制字符串
     * @throws IllegalArgumentException 如果文件为null
     * @throws RuntimeException 如果计算过程中发生错误
     */
    public static String getMD5(File file) {
        return getFileHash(file, MD5);
    }

    /**
     * 计算文件的SHA-1值
     * @param file 文件
     * @return SHA-1哈希值的十六进制字符串
     * @throws IllegalArgumentException 如果文件为null
     * @throws RuntimeException 如果计算过程中发生错误
     */
    public static String getSHA1(File file) {
        return getFileHash(file, SHA1);
    }

    /**
     * 计算文件的SHA-256值
     * @param file 文件
     * @return SHA-256哈希值的十六进制字符串
     * @throws IllegalArgumentException 如果文件为null
     * @throws RuntimeException 如果计算过程中发生错误
     */
    public static String getSHA256(File file) {
        return getFileHash(file, SHA256);
    }

    /**
     * 计算文件的哈希值
     * @param file 文件
     * @param algorithm 哈希算法名称
     * @return 哈希值的十六进制字符串
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果计算过程中发生错误
     */
    private static String getFileHash(File file, String algorithm) {
        if (file == null || algorithm == null) {
            throw new IllegalArgumentException("文件和算法名称不能为null");
        }
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是普通文件");
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(HASH_ERROR + file.getName(), e);
        }
    }

    /**
     * 压缩文件或目录
     * @param source 源文件或目录
     * @param target 目标ZIP文件
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果压缩过程中发生错误
     */
    public static void zip(File source, File target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        if (!source.exists()) {
            throw new IllegalArgumentException("源文件或目录不存在: " + source.getAbsolutePath());
        }

        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException(CREATE_DIR_ERROR + parent.getAbsolutePath());
        }

        try (FileOutputStream fos = new FileOutputStream(target);
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
            
            zos.setLevel(COMPRESSION_LEVEL);
            String basePath = source.getParent();
            if (source.isDirectory()) {
                zipDirectory(source, basePath, zos);
            } else {
                zipFile(source, basePath, zos);
            }
        } catch (IOException e) {
            throw new RuntimeException(ZIP_ERROR + source.getName(), e);
        }
    }

    /**
     * 压缩目录
     */
    private static void zipDirectory(File directory, String basePath, ZipOutputStream zos) 
            throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    zipDirectory(file, basePath, zos);
                } else {
                    zipFile(file, basePath, zos);
                }
            }
        }
    }

    /**
     * 压缩单个文件
     */
    private static void zipFile(File file, String basePath, ZipOutputStream zos) 
            throws IOException {
        String entryName = file.getAbsolutePath().substring(basePath.length() + 1)
            .replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            IoUtils.copy(bis, zos);
        }
        zos.closeEntry();
    }

    /**
     * 解压ZIP文件
     * @param zipFile ZIP文件
     * @param targetDir 目标目录
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果解压过程中发生错误
     */
    public static void unzip(File zipFile, File targetDir) {
        if (zipFile == null || targetDir == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        if (!zipFile.exists() || !zipFile.isFile()) {
            throw new IllegalArgumentException("ZIP文件不存在或不是普通文件");
        }

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException(CREATE_DIR_ERROR + targetDir.getAbsolutePath());
        }

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().replace(UNIX_SEPARATOR, PATH_SEPARATOR);
                File file = new File(targetDir, entryName);
                validatePath(file);

                if (entry.isDirectory()) {
                    if (!file.exists() && !file.mkdirs()) {
                        throw new RuntimeException(CREATE_DIR_ERROR + file.getAbsolutePath());
                    }
                } else {
                    File parent = file.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw new RuntimeException(CREATE_DIR_ERROR + parent.getAbsolutePath());
                    }
                    try (FileOutputStream fos = new FileOutputStream(file);
                         BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(UNZIP_ERROR + zipFile.getName(), e);
        }
    }

    /**
     * 验证文件路径是否合法
     * @param file 文件
     * @throws IllegalArgumentException 如果路径不合法
     */
    private static void validatePath(File file) {
        String path = file.getPath();
        if (path.length() > MAX_PATH_LENGTH) {
            throw new IllegalArgumentException(PATH_TOO_LONG + path);
        }

        // 检查是否包含非法字符
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            for (char c : path.toCharArray()) {
                if (INVALID_PATH_CHARS_WINDOWS.contains(c)) {
                    throw new IllegalArgumentException(INVALID_PATH_CHARS + c);
                }
            }
        }

        // 检查路径是否试图访问父目录
        if (path.contains("..")) {
            throw new IllegalArgumentException("路径不能包含父目录引用: " + path);
        }
    }

    /**
     * 创建临时文件
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @return 临时文件
     * @throws RuntimeException 如果创建失败
     */
    public static File createTempFile(String prefix, String suffix) {
        try {
            return File.createTempFile(
                prefix != null ? prefix : "temp",
                suffix != null ? suffix : ".tmp"
            );
        } catch (IOException e) {
            throw new RuntimeException("创建临时文件失败", e);
        }
    }

    /**
     * 创建临时目录
     * @param prefix 目录名前缀
     * @return 临时目录
     * @throws RuntimeException 如果创建失败
     */
    public static File createTempDirectory(String prefix) {
        try {
            return Files.createTempDirectory(
                prefix != null ? prefix : "temp"
            ).toFile();
        } catch (IOException e) {
            throw new RuntimeException("创建临时目录失败", e);
        }
    }

    /**
     * 获取文件的MIME类型
     * @param file 文件
     * @return MIME类型字符串
     * @throws IllegalArgumentException 如果文件为null
     */
    public static String getMimeType(File file) {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        try {
            String mimeType = Files.probeContentType(file.toPath());
            return mimeType != null ? mimeType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    /**
     * 判断文件是否为文本文件
     * @param file 文件
     * @return true如果是文本文件
     * @throws IllegalArgumentException 如果文件为null
     */
    public static boolean isTextFile(File file) {
        String mimeType = getMimeType(file);
        return mimeType != null && (
            mimeType.startsWith("text/") ||
            mimeType.equals("application/json") ||
            mimeType.equals("application/xml") ||
            mimeType.equals("application/javascript") ||
            mimeType.equals("application/x-yaml") ||
            mimeType.equals("application/x-properties")
        );
    }

    /**
     * 判断文件是否为二进制文件
     * @param file 文件
     * @return true如果是二进制文件
     * @throws IllegalArgumentException 如果文件为null
     */
    public static boolean isBinaryFile(File file) {
        return !isTextFile(file);
    }

    /**
     * 获取文件的最后修改时间
     * @param file 文件
     * @return 最后修改时间的字符串表示
     * @throws IllegalArgumentException 如果文件为null
     */
    public static String getLastModifiedTime(File file) {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        try {
            return Files.getLastModifiedTime(file.toPath()).toString();
        } catch (IOException e) {
            return new Date(file.lastModified()).toString();
        }
    }

    /**
     * 判断文件是否可读写
     * @param file 文件
     * @return true如果文件可读写
     * @throws IllegalArgumentException 如果文件为null
     */
    public static boolean isReadWritable(File file) {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        return file.canRead() && file.canWrite();
    }

    /**
     * 判断文件是否为隐藏文件
     * @param file 文件
     * @return true如果是隐藏文件
     * @throws IllegalArgumentException 如果文件为null
     */
    public static boolean isHidden(File file) {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }
        try {
            return Files.isHidden(file.toPath());
        } catch (IOException e) {
            return file.isHidden();
        }
    }

    /**
     * 写出文件内容
     * @param content 字节数组内容
     * @param fileName 文件名
     * @param path 文件目录，如果为null则使用系统用户目录
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果写入过程中发生错误
     */
    public static void write(byte[] content, String fileName, String path) {
        if (content == null) {
            throw new IllegalArgumentException(NULL_CONTENT);
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_FILENAME);
        }

        File file = createFile(path != null ? path : getSystemHomePath(), fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
            fos.flush();
        } catch (IOException e) {
            throw new RuntimeException(WRITE_ERROR + fileName, e);
        }
    }

    /**
     * 写出文件内容（使用指定字符集）
     * @param content 字符串内容
     * @param fileName 文件名
     * @param path 文件目录，如果为null则使用系统用户目录
     * @param charset 字符集，如果为null则使用UTF-8
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果写入过程中发生错误
     */
    public static void write(String content, String fileName, String path, Charset charset) {
        if (content == null) {
            throw new IllegalArgumentException(NULL_CONTENT);
        }
        write(content.getBytes(charset != null ? charset : DEFAULT_CHARSET), fileName, path);
    }

    /**
     * 获取文件输出流
     * @param fileName 文件名
     * @param path 文件目录，如果为null则使用系统用户目录
     * @return OutputStream 文件输出流
     * @throws IllegalArgumentException 如果文件名为空
     * @throws RuntimeException 如果创建输出流失败
     */
    public static OutputStream getOutputStream(String fileName, String path) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_FILENAME);
        }

        try {
            String actualPath = path == null ? getSystemHomePath() : path;
            return new BufferedOutputStream(
                new FileOutputStream(createFile(actualPath, fileName)), 
                DEFAULT_BUFFER_SIZE
            );
        } catch (IOException e) {
            throw new RuntimeException(CREATE_FILE_ERROR + fileName, e);
        }
    }

    /**
     * 获取系统当前用户目录路径
     * @return 用户目录的绝对路径
     */
    public static String getSystemHomePath() {
        return FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
    }

    /**
     * 路径拼接，智能处理路径分隔符
     * @param rootPath 根路径
     * @param path 子路径
     * @return 拼接后的完整路径
     * @throws IllegalArgumentException 如果任一参数为null
     */
    public static String pathSeparator(String rootPath, String path) {
        if (rootPath == null || path == null) {
            throw new IllegalArgumentException(NULL_PATH);
        }

        rootPath = rootPath.replace(WINDOWS_SEPARATOR, PATH_SEPARATOR)
            .replace(UNIX_SEPARATOR, PATH_SEPARATOR);
        path = path.replace(WINDOWS_SEPARATOR, PATH_SEPARATOR)
            .replace(UNIX_SEPARATOR, PATH_SEPARATOR);

        if (rootPath.endsWith(PATH_SEPARATOR) && path.startsWith(PATH_SEPARATOR)) {
            return rootPath + path.substring(1);
        } else if (!rootPath.endsWith(PATH_SEPARATOR) && !path.startsWith(PATH_SEPARATOR)) {
            return rootPath + PATH_SEPARATOR + path;
        }
        return rootPath + path;
    }

    /**
     * 创建文件，如果目录不存在则创建目录
     * @param mkdirPath 目录路径
     * @param fileName 文件名
     * @return 创建的文件对象
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果创建过程中发生错误
     */
    public static File createFile(String mkdirPath, String fileName) {
        if (mkdirPath == null || fileName == null) {
            throw new IllegalArgumentException("目录路径和文件名不能为null");
        }

        File mkdir = new File(mkdirPath);
        if (!mkdir.exists() && !mkdir.mkdirs()) {
            throw new RuntimeException(CREATE_DIR_ERROR + mkdirPath);
        }

        File file = new File(mkdir, fileName);
        validatePath(file);

        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    throw new RuntimeException(CREATE_FILE_ERROR + file.getAbsolutePath());
                }
            } catch (IOException e) {
                throw new RuntimeException(CREATE_FILE_ERROR + file.getAbsolutePath(), e);
            }
        }
        return file;
    }

    /**
     * 将文件内容转换为字符串列表
     * @param reader 读取字符流
     * @param isClearSpacing 是否清除每行首尾空格
     * @param isClearBlankLines 是否清除空行
     * @param codeBr 换行符，如果为null则不添加
     * @return 文件内容的字符串列表，如果文件为空则返回null
     * @throws IllegalArgumentException 如果Reader为null
     * @throws RuntimeException 如果读取过程中发生错误
     */
    public static List<String> fileToLines(Reader reader, boolean isClearSpacing, 
            boolean isClearBlankLines, String codeBr) {
        if (reader == null) {
            throw new IllegalArgumentException(NULL_READER);
        }

        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (isClearSpacing) {
                    line = line.trim();
                }
                if (isClearBlankLines && line.trim().isEmpty()) {
                    continue;
                }
                if (codeBr != null) {
                    line += codeBr;
                }
                lines.add(line);
            }
            return lines.isEmpty() ? null : lines;
        } catch (IOException e) {
            throw new RuntimeException(READ_ERROR, e);
        }
    }

    /**
     * 将文件内容转换为字符串
     * @param reader 读取字符流
     * @param isClearSpacing 是否清除每行首尾空格
     * @param isClearBlankLines 是否清除空行
     * @param codeBr 换行符，如果为null则不添加
     * @return 文件内容的字符串，如果文件为空则返回null
     * @throws RuntimeException 如果读取过程中发生错误
     */
    public static String fileToString(Reader reader, boolean isClearSpacing, 
            boolean isClearBlankLines, String codeBr) {
        List<String> lines = fileToLines(reader, isClearSpacing, isClearBlankLines, codeBr);
        if (CollectionUtils.isEmpty(lines)) {
            return null;
        }

        StringBuilder builder = new StringBuilder(DEFAULT_BUILDER_CAPACITY);
        for (String line : lines) {
            builder.append(line);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    /**
     * 将文件内容转换为字符串（默认不清除空格和空行，不添加换行符）
     * @param reader 读取字符流
     * @return 文件内容的字符串
     */
    public static String fileToString(Reader reader) {
        return fileToString(reader, false, false, null);
    }

    /**
     * 将文件内容转换为字符串（默认不清除空格和空行）
     * @param reader 读取字符流
     * @param codeBr 换行符
     * @return 文件内容的字符串
     */
    public static String fileToString(Reader reader, String codeBr) {
        return fileToString(reader, false, false, codeBr);
    }

    /**
     * 获取目录下的所有文件
     * @param path 目录路径
     * @param recursive 是否递归获取子目录
     * @param filter 文件过滤器
     * @return 文件列表，如果目录不存在或为空则返回空列表
     * @throws IllegalArgumentException 如果路径为null
     */
    public static List<File> getFiles(String path, boolean recursive, FileFilter filter) {
        if (path == null) {
            throw new IllegalArgumentException(NULL_PATH);
        }

        File directory = new File(path);
        if (!directory.exists() || !directory.isDirectory()) {
            return new ArrayList<>();
        }

        List<File> result = new ArrayList<>();
        if (recursive) {
            try {
                Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        File f = file.toFile();
                        if (filter == null || filter.accept(f)) {
                            result.add(f);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("遍历目录失败: " + path, e);
            }
        } else {
            File[] files = filter == null ? directory.listFiles() : directory.listFiles(filter);
            if (files != null) {
                result.addAll(Arrays.asList(files));
            }
        }
        return result;
    }

    /**
     * 获取目录下的所有文件
     * @param path 目录路径
     * @return 文件列表，如果目录不存在或为空则返回空列表
     */
    public static List<File> getFiles(String path) {
        return getFiles(path, false, null);
    }

    /**
     * 获取目录下的所有文件并按指定方式排序
     * @param path 目录路径
     * @param recursive 是否递归获取子目录
     * @param filter 文件过滤器
     * @param comparator 排序比较器
     * @return 排序后的文件列表
     */
    public static List<File> getFilesSorted(String path, boolean recursive, 
            FileFilter filter, Comparator<File> comparator) {
        List<File> files = getFiles(path, recursive, filter);
        if (comparator != null) {
            files.sort(comparator);
        }
        return files;
    }

    /**
     * 获取目录下的所有文件并按文件名排序
     * @param path 目录路径
     * @param reversed 是否倒序排序
     * @return 排序后的文件列表
     */
    public static List<File> getFilesByNameSort(String path, boolean reversed) {
        Comparator<File> comparator = Comparator.comparing(File::getName);
        return getFilesSorted(path, false, null, 
            reversed ? comparator.reversed() : comparator);
    }

    /**
     * 获取目录下的所有文件并按最后修改时间排序
     * @param path 目录路径
     * @param reversed 是否倒序排序
     * @return 排序后的文件列表
     */
    public static List<File> getFilesByLastModified(String path, boolean reversed) {
        Comparator<File> comparator = Comparator.comparing(File::lastModified);
        return getFilesSorted(path, false, null, 
            reversed ? comparator.reversed() : comparator);
    }

    /**
     * 按扩展名过滤文件
     * @param path 目录路径
     * @param extensions 扩展名数组
     * @return 过滤后的文件列表
     */
    public static List<File> getFilesByExtension(String path, String... extensions) {
        if (extensions == null || extensions.length == 0) {
            return getFiles(path);
        }
        
        Set<String> extSet = Arrays.stream(extensions)
            .filter(Objects::nonNull)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
            
        return getFiles(path, false, file -> 
            !file.isDirectory() && extSet.contains(getExtension(file).toLowerCase()));
    }

    /**
     * 监控目录变化
     * @param path 要监控的目录路径
     * @param recursive 是否监控子目录
     * @param intervalMillis 检查间隔（毫秒）
     * @param callback 文件变化回调函数
     * @return WatchService 监控服务实例
     * @throws IOException 如果创建监控服务失败
     */
    public static WatchService watchDirectory(String path, boolean recursive, 
            long intervalMillis, Consumer<WatchEvent<?>> callback) throws IOException {
        if (path == null || callback == null) {
            throw new IllegalArgumentException("路径和回调函数不能为null");
        }

        Path watchPath = Paths.get(path);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        
        if (recursive) {
            Files.walkFileTree(watchPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) 
                        throws IOException {
                    dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            watchPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        }

        Thread watchThread = new Thread(() -> {
            try {
                while (true) {
                    WatchKey key = watchService.poll(intervalMillis, TimeUnit.MILLISECONDS);
                    if (key == null) {
                        continue;
                    }
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        callback.accept(event);
                    }
                    
                    if (!key.reset()) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        watchThread.setDaemon(true);
        watchThread.start();

        return watchService;
    }

    /**
     * 监控目录变化（使用默认间隔）
     * @param path 要监控的目录路径
     * @param recursive 是否监控子目录
     * @param callback 文件变化回调函数
     * @return WatchService 监控服务实例
     * @throws IOException 如果创建监控服务失败
     */
    public static WatchService watchDirectory(String path, boolean recursive, 
            Consumer<WatchEvent<?>> callback) throws IOException {
        return watchDirectory(path, recursive, DEFAULT_WATCH_INTERVAL, callback);
    }

    /**
     * 复制文件
     * @param source 源文件
     * @param target 目标文件
     * @throws IllegalArgumentException 如果任一参数为null
     * @throws RuntimeException 如果复制过程中发生错误
     */
    public static void copyFile(File source, File target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }

        try {
            Files.copy(source.toPath(), target.toPath(), 
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(COPY_ERROR + source.getAbsolutePath(), e);
        }
    }

    /**
     * 移动文件
     * @param source 源文件
     * @param target 目标文件
     * @throws IllegalArgumentException 如果任一参数为null
     * @throws RuntimeException 如果移动过程中发生错误
     */
    public static void moveFile(File source, File target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }

        try {
            Files.move(source.toPath(), target.toPath(), 
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(MOVE_ERROR + source.getAbsolutePath(), e);
        }
    }

    /**
     * 删除文件或目录
     * @param file 要删除的文件或目录
     * @param recursive 如果是目录，是否递归删除
     * @throws IllegalArgumentException 如果文件为null
     * @throws RuntimeException 如果删除过程中发生错误
     */
    public static void delete(File file, boolean recursive) {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }

        try {
            if (file.isDirectory() && recursive) {
                Files.walk(file.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } else {
                Files.delete(file.toPath());
            }
        } catch (IOException e) {
            throw new RuntimeException(DELETE_ERROR + file.getAbsolutePath(), e);
        }
    }

    /**
     * 获取文件大小的可读字符串表示
     * @param size 文件大小（字节）
     * @return 可读的文件大小字符串
     */
    public static String getReadableFileSize(long size) {
        if (size < KB) {
            return size + " B";
        } else if (size < MB) {
            return String.format("%.2f KB", (double) size / KB);
        } else if (size < GB) {
            return String.format("%.2f MB", (double) size / MB);
        } else if (size < TB) {
            return String.format("%.2f GB", (double) size / GB);
        } else {
            return String.format("%.2f TB", (double) size / TB);
        }
    }

    /**
     * 获取文件扩展名
     * @param file 文件
     * @return 文件扩展名，如果没有扩展名则返回空字符串
     * @throws IllegalArgumentException 如果文件为null
     */
    public static String getExtension(File file) {
        if (file == null) {
            throw new IllegalArgumentException(NULL_FILE);
        }

        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(dotIndex + 1) : "";
    }

    /**
     * 判断文件是否为指定类型
     * @param file 文件
     * @param extensions 扩展名数组
     * @return 如果文件扩展名匹配则返回true
     * @throws IllegalArgumentException 如果参数无效
     */
    public static boolean isFileType(File file, String... extensions) {
        if (file == null || extensions == null || extensions.length == 0) {
            throw new IllegalArgumentException("参数不能为null或空");
        }

        String fileExt = getExtension(file).toLowerCase();
        for (String ext : extensions) {
            if (ext != null && ext.toLowerCase().equals(fileExt)) {
                return true;
            }
        }
        return false;
    }
}
