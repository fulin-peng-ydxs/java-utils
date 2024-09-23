package image.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**二维码工具类
 *@author fulin-peng
 *2024-09-20  09:42
 */
public class QRUtils {



    /**
     * 生成二维码
     * @param content 二维码内容
     * @param width 二维码宽度
     * @param height 二维码长度
     * @param errorCorrectionLevel 二维码纠错级别
     * @param logo 二维码中心图标 类型可以为Path或InputStream
     * @param formatName 二维码图片格式：JPG、PNG等
     * @param target 二维码输出目标：类型可以为Path或OutputStream
     * 2024/9/20 上午9:43
     * @author fulin-peng
     */
    public static void generateQRCode(String content, int width, int height,ErrorCorrectionLevel errorCorrectionLevel,String formatName,Object logo,Object target) {
        try{
            // 创建 QRCodeWriter 实例
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            // 配置二维码参数（编码格式等）
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel); // 设置高纠错级别，让二维码在 30% 的数据损坏下仍然可以被识别

            // 生成二维码的 BitMatrix
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            if(logo==null){
                // 将二维码的矩阵转换为图片
                if(target instanceof Path){
                    MatrixToImageWriter.writeToPath(bitMatrix, formatName,(Path)target);
                }else {
                    MatrixToImageWriter.writeToStream(bitMatrix, formatName, (OutputStream) target);
                }
            }else{
                // 默认情况下，二维码是黑白的，但通过 new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                // 可以生成彩色图像，以保持二维码和 Logo 都是彩色的。
                BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

                //使用 BufferedImage.setRGB 方法将 BitMatrix 中的二维码数据填充到彩色图像中
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        qrImage.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                    }
                }

                // 加载 Logo 图片
                BufferedImage logoImage = logo instanceof Path?ImageIO.read(((Path)logo).toFile()):
                        ImageIO.read((InputStream)logo);
                // 在二维码的中心绘制 Logo
                Graphics2D g = qrImage.createGraphics();
                // 启用抗锯齿和高质量渲染
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); // 双线性插值，防止黑边
                // 计算 Logo 大小，约为二维码的1/5
                int logoWidth = qrImage.getWidth() / 5;
                int logoHeight = qrImage.getHeight() / 5;
                int x = (qrImage.getWidth() - logoWidth) / 2;
                int y = (qrImage.getHeight() - logoHeight) / 2;
                //绘制logo
                g.drawImage(logoImage, x, y, logoWidth, logoHeight, null);
                // 设置圆角边框，避免 Logo 和二维码部分发生冲突
                BasicStroke stroke = new BasicStroke(2); // 设置边框粗细
                g.setStroke(stroke);
                g.setColor(Color.WHITE); // 边框颜色为白色
                g.drawRoundRect(x, y, logoWidth, logoHeight, 15, 15); // 绘制圆角边框

                g.dispose();  // 释放资源

                // 将带有 Logo 的二维码写出
                if(target instanceof Path){
                    ImageIO.write(qrImage, formatName,((Path)target).toFile());
                }else{
                    ImageIO.write(qrImage, formatName, (OutputStream) target);
                }
            }
        }catch (Exception e){
            throw new RuntimeException("生成二维码异常",e);
        }
    }


    /**
     * 生成二维码 （根据二维码尺寸和logo有无动态调整容错率）
     * @param content 二维码内容
     * @param width 二维码宽度
     * @param height 二维码长度
     * @param logo 二维码中心图标 类型可以为Path或InputStream
     * @param formatName 二维码图片格式：JPG、PNG等
     * @param target 二维码输出目标：类型可以为Path或InputStream
     * 2024/9/20 上午9:43
     * @author fulin-peng
     */
    public static void generateQRCode(String content, int width, int height,String formatName,Object logo,Object target){
        generateQRCode(content,width,height,logo==null?ErrorCorrectionLevel.L:ErrorCorrectionLevel.H,formatName,logo,target);
    }
}
