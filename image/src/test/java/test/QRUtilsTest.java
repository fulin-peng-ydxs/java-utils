package test;

import image.qr.QRUtils;

import java.nio.file.FileSystems;
import java.nio.file.Path;

/**QRUtils工具类测试
 *@author fulin-peng
 *2024-09-20  14:22
 */
public class QRUtilsTest {

    public static void main(String[] args) {
        // 调用生成二维码方法
        String qrCodeText = "https://docs.qq.com/sheet/DQkJZSVVaVUZNRFVP?tab=brwrma";
        int width = 300;
        int height = 300;
        Path logoPath = FileSystems.getDefault().getPath("E:\\java-utils\\image\\src\\main\\resources\\image\\封面.png");
        Path qrCodePath = FileSystems.getDefault().getPath("C:\\Users\\Administrator\\Desktop\\qr_code.png");
        QRUtils.generateQRCode(qrCodeText, width, height,"PNG",logoPath,qrCodePath);
    }
}
