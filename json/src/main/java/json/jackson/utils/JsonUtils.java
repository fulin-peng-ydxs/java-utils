package json.jackson.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 	json的工具类	
 */
public abstract class JsonUtils {

	/**
	 * json-序列化
	 * @author fulin peng
	 * @param prettyPrinter 是否需要美化打印
	 * 2023/8/3 0003 11:47
	 */
	public static String getString(Object object,boolean prettyPrinter) {
		ObjectMapper mapper =getMapper(true);
		ObjectWriter ow = mapper.writer();
		if(prettyPrinter){
			ow.withDefaultPrettyPrinter();
		}
		try {
			return ow.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("json-序列化异常",e);
		}
	}

	/**
	 * json-序列化
	 * @author fulin peng
	 * 2023/8/3 0003 11:52
	 */
	public static String getString(Object object) {
		return getString(object,false);
	}



	/**
	 * json-反序列化:object-object
	 * @author fulin peng
	 * 2023/8/3 0003 11:57
	 */
	public static <T> T  getObject(Object object,Class<T> valueType){
		return getObject(getString(object), valueType);
	}


	/**
	 * json-反序列化:string-object
	 * @author fulin peng
	 * 2023/8/3 0003 12:03
	 */
	public static <T> T getObject(String str, Class<T> valueType) {
		try {
			return  getMapper(true).readValue(str, valueType);
		} catch (IOException e) {
			throw new RuntimeException("json-序列化异常",e);
		}
	}

	/**
	 * json-反序列化:string-list
	 * @author fulin peng
	 * 2023/8/3 0003 12:08
	 */
	public static <T> List<T> getObjects(String str, TypeReference<List<T>> type){
		try {
			return getMapper(true).readValue(str,type);
		} catch (Exception e) {
			throw new RuntimeException("json-序列化异常",e);
		}
	}


	/**
	 * json-反序列化:stream-object
	 * @author fulin peng
	 * 2023/8/3 0003 12:10
	 */
	public static <T> T getObject(InputStream stream, Class<T> valueType) {
		try {
			return getMapper(true).readValue(stream,valueType);
		} catch (IOException e) {
			throw new RuntimeException("json-序列化异常",e);
		}
	}

	/**
	 * json-反序列化-stream-list
	 * @author fulin peng
	 * 2023/8/3 0003 12:10
	 */
	public static <T> List<T> getObjects(InputStream stream,TypeReference<List<T>> type) {
		try {
			return getMapper(true).readValue(stream,type);
		} catch (IOException e) {
			throw new RuntimeException("json-序列化异常",e);
		}
	}

	/**
	 * 返回mapper序列化器
	 * @author fulin peng
	 * 2023/8/3 0003 11:59
	 */
	public static ObjectMapper getMapper(boolean ignoreInExistence){
		ObjectMapper mapper =new ObjectMapper();
		if(ignoreInExistence)
			// 如果json中有新增的字段并且是实体类类中不存在的，不报错
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}
}
