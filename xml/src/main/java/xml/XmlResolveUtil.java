package xml;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Xml解析工具
 *
 * @author peng_fu_lin
 * 2022-10-10 15:21
 */
public class XmlResolveUtil {

    /**解析xml-Dom元素
     * 2022/10/10 0010-15:18
     * @author pengfulin
     */
    public static Document getDom(String path) throws FileNotFoundException, DocumentException {
        InputStream inputStream=null;
        if(path.startsWith("classpath:"))
            inputStream= XmlResolveUtil.class.getResourceAsStream(path.replace("classpath:", ""));
        else
            inputStream=new FileInputStream(path);
        SAXReader saxReader = new SAXReader(); //xml声明自定义
        saxReader.setEntityResolver(new IgnoreDTDEntityResolver());
        return saxReader.read(inputStream)  ;
    }

    /**解析xml-Element
     * 2022/10/10 0010-15:28
     * @author pengfulin
     */
    public static Element getRootElement(String path) throws DocumentException, FileNotFoundException {
        return getDom(path).getRootElement();
    }

    static class IgnoreDTDEntityResolver implements EntityResolver {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return new InputSource(new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes()));
        }
    }
}
