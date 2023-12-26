package httpclient.utils;

import json.jackson.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * httpclient调用工具类
 *
 * @author peng_fu_lin
 * 2023-06-15 14:51
 */
@Slf4j
public abstract class HttpClientUtil {

    /**通用请求执行
     * 2023/6/15 0015-14:53
     * @author pengfulin
     * @param requestType 请求方法
     * @param url 请求地址
     * @param params 请求参数
     * @param headers 请求头
     * @param targetType 请求结果类型
     * @param targetName 请求响应结果中的结果集属性名
     * @param statusName 请求响应结果中的结果状态属性名
     * @param statusValue 请求响应结果中的正常结果状态值
     * @param errorName 请求响应结果中的错误消息属性名
     */
    public static  <T> T execute(RequestType requestType, String url, Map<String,Object> params,Map<String,String> headers, Class<T> targetType,
                                 String targetName, String statusName, String statusValue, String errorName) throws Exception {
        try (CloseableHttpClient httpClient = createHttpClient(false)) {
            return execute(httpClient,requestType,url,params,headers,targetType,targetName,statusName,statusValue,errorName);
        }
    }

    /**
     * 通用请求执行
     * 2023/8/3 0003 11:33
     * @author fulin peng
     * @author httpClient 使用自定义的httpclient对象
     * @param httpClient 请求客户端
     * @param requestType 请求方法
     * @param url 请求地址
     * @param params 请求参数
     * @param headers 请求头
     * @param targetType 请求结果类型
     * @param targetName 请求响应结果中的结果集属性名
     * @param statusName 请求响应结果中的结果状态属性名
     * @param statusValue 请求响应结果中的正常结果状态值
     * @param errorName 请求响应结果中的错误消息属性名
     */
    public static  <T> T execute(HttpClient httpClient,RequestType requestType, String url, Map<String,Object> params,Map<String,String> headers, Class<T> targetType,
                                 String targetName, String statusName, String statusValue, String errorName) throws Exception {
        HttpUriRequest httpRequest=null;
        if(requestType==RequestType.GET){  //GET请求
            //请求url参数添加
            if(params!=null&& !params.isEmpty()){
                URIBuilder uriBuilder = new URIBuilder(url);
                params.forEach((key,value)->{
                    uriBuilder.setParameter(key, (String) value);
                });
                url=uriBuilder.build().toString();
            }
            log.debug("HttpClient-Get调用服务：{}",url);
            httpRequest= new HttpGet(url);
        }else{ //POST&其他请求
            String contentType =null;
            boolean isNotJson=headers!=null && (contentType = headers.get("Content-Type"))!=null && !contentType.equals(RequestDataType.APPLICATION_JSON.value);
            HttpPost httpPost= new HttpPost(url);
            httpRequest= httpPost;
            if(isNotJson) {
                if (contentType.equals(RequestDataType.URL_ENCODED_FORM.value)) {  //url编码表单处理
                    // 添加请求参数
                    List<NameValuePair> urlParameters = new ArrayList<>();
                    params.forEach((key,value)->{
                        urlParameters.add(new BasicNameValuePair(key, value.toString()));
                    });
                    log.debug("HttpClient-Post(UrlEncodedForm)调用服务：url:{},params:{}",url,urlParameters);
                    httpPost.setEntity(new UrlEncodedFormEntity(urlParameters));
                }
            }else {
                String paramJson = JsonUtils.getString(params);
                log.debug("HttpClient-Post(Json)调用服务：url:{},params:{}",url,paramJson);
                httpPost.setEntity(new StringEntity(paramJson, StandardCharsets.UTF_8));
                //设置请求头
                httpPost.setHeader("Content-Type","application/json");
            }
        }
        if(headers!=null){
            for (Map.Entry<String,String> headersEntry : headers.entrySet()) {
                httpRequest.setHeader(headersEntry.getKey(),headersEntry.getValue());
            }
        }
        //执行请求
        return abstractResponse(httpClient.execute(httpRequest),targetType,targetName,statusName,statusValue,errorName);
    }


    /**
     * Post请求执行
     * 2023/10/15 23:40
     * @return 以json字符串方式返回响应信息
     * @author pengshuaifeng
     */
    public static String executePost(String url, Map<String,Object> params,Map<String,String> headers,
                                     String targetName, String statusName, String statusValue, String errorName) throws Exception {
        return execute(RequestType.POST, url, params, headers,String.class,targetName, statusName, statusValue, errorName);
    }

    /**
     * Post请求执行
     * 2023/10/15 23:40
     * @return targetType类型的对象
     * @author pengshuaifeng
     */
    public static <T> T executePost(String url, Map<String,Object> params,Map<String,String> headers,Class<T> targetType,
                                    String targetName, String statusName, String statusValue, String errorName) throws Exception {
        return execute(RequestType.POST, url, params, headers,targetType,targetName, statusName, statusValue, errorName);
    }

    /**
     * url编码表单请求
     * 2023/11/28 0028 11:05
     * @author fulin-peng
     */
    public static <T> T executeUrlEncodedForm(String url, Map<String,Object> params,Map<String,String> headers,Class<T> targetType,
                                              String targetName,String statusName, String statusValue, String errorName) throws Exception {
        if(headers==null)
            headers= new HashMap<>();
        headers.put("Content-Type",RequestDataType.URL_ENCODED_FORM.value);
        return execute(RequestType.POST, url, params,headers,targetType,targetName, statusName, statusValue, errorName);
    }


    /**
     * Get请求执行
     * 2023/10/15 23:40
     * @return 以json字符串方式返回响应信息
     * @author pengshuaifeng
     */
    public static String executeGET(String url, Map<String,Object> params,Map<String,String> headers,
                                    String targetName, String statusName, String statusValue, String errorName) throws Exception {
        return execute(RequestType.GET, url, params,headers,String.class,targetName, statusName, statusValue, errorName);
    }
    /**
     * Get请求执行
     * 2023/10/15 23:40
     * @return targetType类型的对象
     * @author pengshuaifeng
     */
    public static <T> T executeGET(String url, Map<String,Object> params,Map<String,String> headers,Class<T> targetType,
                                   String targetName, String statusName, String statusValue, String errorName) throws Exception {
        return execute(RequestType.GET, url, params,headers,targetType,targetName, statusName, statusValue, errorName);
    }

    /**文件请求上传
     * 2023/6/15 0015-14:54
     * @author pengfulin
     * @param url 请求地址
     * @param inputStream 文件流
     * @param headers 请求头
     * @param targetType 需要的请求结果类型
     * @param targetName 请求响应结果中的结果集属性名
     * @param statusName 请求响应结果中的结果状态属性名
     * @param statusValue 请求响应结果中的正常结果状态值
     * @param errorName 请求响应结果中的错误消息属性名
     * @return targetType类型的对象
     */
    public static <T> T executeUpload(String url, InputStream inputStream,Map<String,String> headers,Class<T> targetType,
                                      String targetName,String statusName, String statusValue, String errorName, String fileName) throws Exception {
        try (CloseableHttpClient httpClient = createHttpClient(false)) {
            HttpPost httpPost = new HttpPost(url);
            //设置请求头
            if(headers!=null){
                for (Map.Entry<String,String> headersEntry : headers.entrySet()) {
                    httpPost.setHeader(headersEntry.getKey(),headersEntry.getValue());
                }
            }
            //设置请求体：多部分数据构建
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            //添加参数：文件，并设置数据类型：二进制类型
            /*application/octet-stream 是一种 MIME 类型（Multipurpose Internet Mail Extensions），它通常用于表示二进制数据文件的内容类型。
            这个 MIME 类型没有特定的数据格式或结构，它通常用于指示数据是未知的、不可解释的二进制数据。
            application/octet-stream 可以用于描述各种类型的文件，包括但不限于：未知文件类型的二进制数据。二进制文件，如图像、音频、视频文件。 压缩文件，如 ZIP、GZIP、TAR 文件。 可执行文件，如可执行程序或脚本文件。
            当您收到一个 HTTP 响应，其内容类型被标记为 application/octet-stream 时，这意味着服务器正在传输二进制数据，但它不提供有关数据内容的详细信息。
            通常，这种情况下，您需要根据您的应用程序的需要来处理这些数据，例如，将它们保存到文件或执行其他操作。
            application/octet-stream 的主要作用是通知接收端，它不应该尝试解释数据内容，而应该将数据保存为原始的二进制形式。这对于传输各种文件和数据类型非常有用，因为它确保数据的完整性和保密性。*/
            entityBuilder.addBinaryBody("file",inputStream,
                    ContentType.APPLICATION_OCTET_STREAM,fileName);
            //设置请求体模式：浏览器兼容模式，即只写"Content-Disposition";使用内容字符集
            entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            //设置请求体字符集
            entityBuilder.setCharset(StandardCharsets.UTF_8);
            httpPost.setEntity(entityBuilder.build());
            log.debug("HttpClient文件上传调用服务：{}",url);
            //执行请求
            return abstractResponse(httpClient.execute(httpPost), targetType, targetName, statusName, statusValue, errorName);
        }
    }

    /**
     * 多部分请求
     * 2023/11/28 0028 11:05
     * @author fulin-peng
     */

    /**响应解析
     * 2023/6/15 0015-15:09
     * @throws RuntimeException 如果响应状态不为200，则抛出响应信息
     * @author pengfulin
     */
    private static <T> T abstractResponse(HttpResponse response,Class<T> targetType, String targetName, String statusName, String statusValue, String errorName)
            throws Exception{
        String responseJson = EntityUtils.toString(response.getEntity(), "UTF-8");
        log.debug("HttpClient调用结果：{}",responseJson);
        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode==500)
            throw new RuntimeException("远程服务调用异常："+responseJson);
        //如果targetName&statusName均为空，则直接将响应结果转换成targetType类型直接返回
        if(targetName==null&&statusName==null)
            return JsonUtils.getObject(responseJson,targetType);
        //否则，根据响应提供信息进一步解析响应信息
        return abstractResponse(responseJson,targetType,targetName,statusName,statusValue,errorName);
    }

    /**
     * 响应解析
     * 2023/10/16 00:25
     * @throws RuntimeException 如果响应状态不为200，则抛出响应信息
     * @author pengshuaifeng
     */
    private static <T> T abstractResponse(String responseJson,Class<T> targetType, String targetName, String statusName, String statusValue, String errorName) {
        Map<?,?> result = (Map<?,?>)JsonUtils.getObject(responseJson, Map.class);
        Object resStatus = result.get(statusName);
        if(resStatus==null|| resStatus.toString().isEmpty())
            throw new RuntimeException("响应状态缺失："+responseJson);
        else if(!resStatus.toString().equals(statusValue)){
            String error =(String) result.get(errorName);
            throw new RuntimeException("请求失败："+error);
        }
        if(targetName==null)  //如果结果名为空，则解析整个响应信息
            return JsonUtils.getObject(responseJson,targetType);
        Object resData =result.get(targetName);
        if(resData==null)
            throw new RuntimeException("响应数据缺失或为空："+responseJson);
        return JsonUtils.getObject(resData,targetType);
    }

    /**
     * 文件请求下载
     * 2023/10/16 00:10
     * @author pengshuaifeng
     */


    /**
     * 创建HttpClient
     * 2023/10/18 01:45
     * @param ignoreSSl 忽略证书，用于https协议不安全调用
     * @author pengshuaifeng
     */
    public static CloseableHttpClient createHttpClient(boolean ignoreSSl) throws Exception{
        if(ignoreSSl){
            // 创建不验证证书的 SSL 上下文
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true)
                    .build();
            // 创建 HttpClient，并禁用 SSL 验证
            return HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        }else{
            return HttpClients.createDefault();
        }
    }


    /**
     * 请求类型
     * 2023/10/15 22:41
     * @author pengshuaifeng
     */
    public enum RequestType{
        GET,
        POST
    }

    /**
     * 请求数据类型
     * 2023/12/11 00:17
     * @author pengshuaifeng
     */
    public enum RequestDataType {

        URL_ENCODED_FORM("application/x-www-form-urlencoded"),

        APPLICATION_JSON("application/json");

        public final String value;

        RequestDataType(String value) {
            this.value = value;
        }
    }

}