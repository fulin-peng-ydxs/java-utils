package httpclient.file;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.File;

/**
 * 文件上传案例
 * 2023/10/16 00:59
 * @author pengshuaifeng
 */
public class FileUploadExample {

    public static void main(String[] args) throws Exception {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("https://example.com/upload");

        // 创建一个 MultipartEntityBuilder 来构建 HTTP POST 请求的实体
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        // 添加文件到实体
        File file = new File("example.jpg");
        builder.addBinaryBody("file", file, ContentType.DEFAULT_BINARY, "example.jpg");

        // 构建实体
        HttpEntity entity = builder.build();
        httpPost.setEntity(entity);

        // 执行 HTTP POST 请求
        HttpResponse response = httpClient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println("HTTP Status Code: " + statusCode);
        /*
          在上述示例中，httpmime 模块用于创建 MultipartEntity 对象，将文件添加到 HTTP POST 请求中，然后执行请求以上传文件。
          请注意，您需要在项目的 Maven POM 文件中添加对 httpmime 的依赖才能使用这个模块。这通常是为需要处理文件上传和多媒体数据
          上传的应用程序添加额外功能的情况。
        */
    }
}
