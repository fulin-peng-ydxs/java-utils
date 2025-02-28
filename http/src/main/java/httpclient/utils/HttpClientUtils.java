package httpclient.utils;

import json.jackson.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * HTTP客户端工具类，提供同步和异步HTTP请求功能
 *
 * @author pengshuaifeng
 * @since 2023/6/15
 */
@Slf4j
public abstract class HttpClientUtils {

    private HttpClientUtils() {
        // Utility class should not be instantiated
    }

    /** 默认配置 */
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 10000;
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 3000;
    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 200;
    private static final int DEFAULT_MAX_PER_ROUTE_CONNECTIONS = 20;
    private static final int DEFAULT_KEEP_ALIVE_TIME = 20000;
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int DEFAULT_RETRY_INTERVAL = 1000;
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /** 连接池管理器 */
    private static final PoolingHttpClientConnectionManager connectionManager;

    /** 异步HTTP客户端 */
    private static final CloseableHttpAsyncClient asyncHttpClient;

    static {
        try {
            // SSL配置
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true)
                    .build();
            SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(
                    sslContext, NoopHostnameVerifier.INSTANCE);

            // 注册HTTP和HTTPS协议
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslFactory)
                    .build();

            // 初始化连接池
            connectionManager = new PoolingHttpClientConnectionManager(registry);
            connectionManager.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
            connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE_CONNECTIONS);

            // 初始化异步客户端
            asyncHttpClient = HttpAsyncClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
            asyncHttpClient.start();

            // 启动空闲连接清理
            startIdleConnectionMonitor();
        } catch (Exception e) {
            throw new RuntimeException("初始化HTTP客户端失败", e);
        }
    }

    /**
     * 创建HTTP客户端
     */
    public static CloseableHttpClient createHttpClient() {
        return createHttpClient(true, createDefaultRequestConfig());
    }

    /**
     * 创建HTTP客户端
     * @param ignoreSSL 是否忽略SSL证书验证
     * @param requestConfig 请求配置
     */
    public static CloseableHttpClient createHttpClient(boolean ignoreSSL, RequestConfig requestConfig) {
        try {
            HttpClientBuilder builder = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .setKeepAliveStrategy(createKeepAliveStrategy())
                    .setRetryHandler(createRetryHandler());

            if (ignoreSSL) {
                SSLContext sslContext = SSLContextBuilder.create()
                        .loadTrustMaterial((chain, authType) -> true)
                        .build();
                builder.setSSLContext(sslContext)
                       .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("创建HTTP客户端失败", e);
        }
    }

    /**
     * 创建默认请求配置
     */
    private static RequestConfig createDefaultRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
                .build();
    }

    /**
     * 创建Keep-Alive策略
     */
    private static ConnectionKeepAliveStrategy createKeepAliveStrategy() {
        return (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator(
                    response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return DEFAULT_KEEP_ALIVE_TIME;
        };
    }

    /**
     * 创建重试处理器
     */
    private static HttpRequestRetryHandler createRetryHandler() {
        return (exception, executionCount, context) -> {
            if (executionCount >= DEFAULT_RETRY_COUNT) {
                return false;
            }
            try {
                Thread.sleep(DEFAULT_RETRY_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        };
    }

    /**
     * 启动空闲连接监控
     */
    private static void startIdleConnectionMonitor() {
        Thread monitor = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (connectionManager) {
                        connectionManager.closeExpiredConnections();
                        connectionManager.closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        monitor.setDaemon(true);
        monitor.start();
    }

    /**
     * 异步执行HTTP请求
     */
    public static <T> CompletableFuture<T> executeAsync(RequestType requestType, String url,
            Map<String, Object> params, Map<String, String> headers, Class<T> targetType,
            String targetName, String statusName, String statusValue, String errorName) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            HttpUriRequest request = createRequest(requestType, url, params, headers);
            asyncHttpClient.execute(request, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    try {
                        T response = abstractResponse(result, targetType, targetName, 
                            statusName, statusValue, errorName);
                        future.complete(response);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }

                @Override
                public void failed(Exception ex) {
                    future.completeExceptionally(ex);
                }

                @Override
                public void cancelled() {
                    future.cancel(true);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 创建HTTP请求
     */
    private static HttpUriRequest createRequest(RequestType requestType, String url,
            Map<String, Object> params, Map<String, String> headers) throws Exception {
        HttpUriRequest request;
        
        switch (requestType) {
            case GET:
                request = new HttpGet(buildUrlWithParams(url, params));
                break;
            case POST:
                HttpPost post = new HttpPost(url);
                setEntityForPost(post, params, headers);
                request = post;
                break;
            case PUT:
                HttpPut put = new HttpPut(url);
                setEntityForPost(put, params, headers);
                request = put;
                break;
            case DELETE:
                request = new HttpDelete(buildUrlWithParams(url, params));
                break;
            case PATCH:
                HttpPatch patch = new HttpPatch(url);
                setEntityForPost(patch, params, headers);
                request = patch;
                break;
            default:
                throw new IllegalArgumentException("不支持的请求类型: " + requestType);
        }

        // 设置请求头
        if (headers != null) {
            headers.forEach(request::setHeader);
        }

        return request;
    }

    /**
     * 为POST类请求设置实体
     */
    private static void setEntityForPost(HttpEntityEnclosingRequestBase request,
            Map<String, Object> params, Map<String, String> headers) throws Exception {
        if (params == null || params.isEmpty()) {
            return;
        }

        String contentType = headers != null ? headers.get("Content-Type") : null;
        if (contentType != null && contentType.contains(MimeType.URL_ENCODED_FORM)) {
            List<NameValuePair> parameters = new ArrayList<>();
            params.forEach((key, value) -> 
                parameters.add(new BasicNameValuePair(key, String.valueOf(value))));
            request.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));
        } else {
            String jsonParams = JsonUtils.getString(params);
            request.setEntity(new StringEntity(jsonParams, StandardCharsets.UTF_8));
            request.setHeader("Content-Type", MimeType.APPLICATION_JSON);
        }
    }

    /**
     * 构建带参数的URL
     */
    private static String buildUrlWithParams(String url, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        StringBuilder builder = new StringBuilder(url);
        if (!url.contains("?")) {
            builder.append("?");
        } else if (!url.endsWith("&")) {
            builder.append("&");
        }

        params.forEach((key, value) -> 
            builder.append(key).append("=").append(value).append("&"));
        return builder.substring(0, builder.length() - 1);
    }

    /**
     * 执行HTTP请求
     */
    public static <T> T execute(RequestType requestType, String url, Map<String, Object> params,
            Map<String, String> headers, Class<T> targetType, String targetName, String statusName,
            String statusValue, String errorName) throws Exception {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            return execute(httpClient, requestType, url, params, headers, targetType,
                    targetName, statusName, statusValue, errorName);
        }
    }

    /**
     * 使用指定客户端执行HTTP请求
     */
    public static <T> T execute(HttpClient httpClient, RequestType requestType, String url,
            Map<String, Object> params, Map<String, String> headers, Class<T> targetType,
            String targetName, String statusName, String statusValue, String errorName) throws Exception {
        HttpUriRequest request = createRequest(requestType, url, params, headers);
        log.debug("HTTP请求: {} {}", requestType, url);
        if (params != null) {
            log.debug("请求参数: {}", params);
        }
        if (headers != null) {
            log.debug("请求头: {}", headers);
        }
        return abstractResponse(httpClient.execute(request), targetType, targetName,
                statusName, statusValue, errorName);
    }

    /**
     * 执行文件上传请求
     */
    public static <T> T executeUpload(String url, InputStream inputStream,
            Map<String, String> headers, Class<T> targetType, String targetName,
            String statusName, String statusValue, String errorName, String fileName) throws Exception {
        return executeMultipart(url, null, headers, targetType, targetName,
                statusName, statusValue, errorName, inputStream, fileName);
    }

    /**
     * 执行多部分请求
     */
    public static <T> T executeMultipart(String url, Map<String, Object> params,
            Map<String, String> headers, Class<T> targetType, String targetName,
            String statusName, String statusValue, String errorName,
            InputStream fileInputStream, String fileName) throws Exception {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost httpPost = new HttpPost(url);
            
            if (headers != null) {
                headers.forEach(httpPost::setHeader);
            }

            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE) //设置请求体模式：浏览器兼容模式，即只写"Content-Disposition";使用内容字符集
                    .setCharset(StandardCharsets.UTF_8); //设置请求体字符集

            if (fileInputStream != null) {
                //添加参数：文件，并设置数据类型：二进制类型
                /*application/octet-stream 是一种 MIME 类型（Multipurpose Internet Mail Extensions），它通常用于表示二进制数据文件的内容类型。
                这个 MIME 类型没有特定的数据格式或结构，它通常用于指示数据是未知的、不可解释的二进制数据。
                application/octet-stream 可以用于描述各种类型的文件，包括但不限于：未知文件类型的二进制数据。二进制文件，如图像、音频、视频文件。 压缩文件，如 ZIP、GZIP、TAR 文件。 可执行文件，如可执行程序或脚本文件。
                当您收到一个 HTTP 响应，其内容类型被标记为 application/octet-stream 时，这意味着服务器正在传输二进制数据，但它不提供有关数据内容的详细信息。
                通常，这种情况下，您需要根据您的应用程序的需要来处理这些数据，例如，将它们保存到文件或执行其他操作。
                application/octet-stream 的主要作用是通知接收端，它不应该尝试解释数据内容，而应该将数据保存为原始的二进制形式。这对于传输各种文件和数据类型非常有用，因为它确保数据的完整性和保密性。*/
 
                builder.addBinaryBody("file", fileInputStream,
                        ContentType.APPLICATION_OCTET_STREAM, fileName);
            }

            if (params != null) {
                params.forEach((key, value) -> builder.addBinaryBody(key,
                        String.valueOf(value).getBytes(StandardCharsets.UTF_8)));
            }

            httpPost.setEntity(builder.build());
            log.debug("多部分请求: {}", url);
            
            return abstractResponse(httpClient.execute(httpPost), targetType,
                    targetName, statusName, statusValue, errorName);
        }
    }

    /**
     * 解析HTTP响应
     */
    private static <T> T abstractResponse(HttpResponse response, Class<T> targetType,
            String targetName, String statusName, String statusValue, String errorName)
            throws Exception {
        int statusCode = response.getStatusLine().getStatusCode();
        Header contentType = response.getFirstHeader("Content-Type");
        HttpEntity entity = response.getEntity();

        if (responseIsNormal(statusCode) && contentType != null &&
                contentType.getValue().contains(MimeType.APPLICATION_OCTET_STREAM)) {
            return handleBinaryResponse(response, targetType);
        }

        String responseJson = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        log.debug("HTTP响应: {}", responseJson);

        if (!responseIsNormal(statusCode)) {
            throw new RuntimeException("HTTP请求失败: " + responseJson);
        }

        if (targetName == null && statusName == null) {
            return JsonUtils.getObject(responseJson, targetType);
        }

        return parseJsonResponse(responseJson, targetType, targetName,
                statusName, statusValue, errorName);
    }

    /**
     * 处理二进制响应
     */
    @SuppressWarnings("unchecked")
    private static <T> T handleBinaryResponse(HttpResponse response, Class<T> targetType)
            throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getEntity().writeTo(outputStream);

        if (targetType == ByteArrayOutputStream.class) {
            return (T) outputStream;
        }

        FileResponse fileResponse = new FileResponse();
        fileResponse.setContent(outputStream);

        Header contentDisposition = response.getFirstHeader("Content-Disposition");
        if (contentDisposition != null) {
            String[] parts = contentDisposition.getValue().split(";");
            for (String part : parts) {
                part = part.trim();
                if (part.toLowerCase().startsWith("filename=")) {
                    String fileName = part.substring(9).replace("\"", "");
                    fileResponse.setFileName(URLDecoder.decode(fileName, "UTF-8"));
                    break;
                }
            }
        }

        return (T) fileResponse;
    }

    /**
     * 解析JSON响应
     */
    private static <T> T parseJsonResponse(String responseJson, Class<T> targetType,
            String targetName, String statusName, String statusValue, String errorName) {
        Map<?, ?> result = JsonUtils.getObject(responseJson, Map.class);
        
        Object status = result.get(statusName);
        if (status == null || status.toString().isEmpty()) {
            throw new RuntimeException("响应状态缺失: " + responseJson);
        }
        
        if (!status.toString().equals(statusValue)) {
            String error = (String) result.get(errorName);
            throw new RuntimeException("请求失败: " + error);
        }

        if (targetName == null) {
            return null;
        }

        if ("all-data".equals(targetName)) {
            return JsonUtils.getObject(responseJson, targetType);
        }

        Object data = result.get(targetName);
        if (responseIsEmpty(data)) {
            throw new RuntimeException("响应数据为空: " + responseJson);
        }

        return JsonUtils.getObject(data, targetType);
    }

    /**
     * 检查响应是否为空
     */
    public static boolean responseIsEmpty(Object data) {
        return data == null || "null".equals(data.toString()) ||
               "{}".equals(data.toString()) || "[]".equals(data.toString());
    }

    /**
     * 检查响应状态是否正常
     */
    public static boolean responseIsNormal(HttpResponse response) {
        return responseIsNormal(response.getStatusLine().getStatusCode());
    }

    /**
     * 检查状态码是否正常
     */
    public static boolean responseIsNormal(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * HTTP请求类型
     */
    public enum RequestType {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH
    }

    /**
     * MIME类型常量
     */
    public static class MimeType {
        public static final String URL_ENCODED_FORM = "application/x-www-form-urlencoded";
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
        
        private MimeType() {
            // Constants class
        }
    }

    /**
     * 文件响应对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileResponse {
        private String fileName;
        private ByteArrayOutputStream content;
    }
}
