package com.javatao.rest.client.proxy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.javatao.rest.client.annotations.Param;
import com.javatao.rest.client.exception.ExceptionUtil;
import com.javatao.rest.client.utils.FkUtils;
import com.javatao.rest.client.utils.RestUtils;
import com.javatao.rest.client.vo.ApiResponse;

/**
 * 代理处理类
 * 
 * @author TLF
 */
public class IfaceProxy implements InvocationHandler, Serializable {
    private static final Log logger = LogFactory.getLog(IfaceProxy.class);
    private static final long serialVersionUID = -707292751479136782L;
    private String basedir = "";

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            String templateName = getTemplateName(method);
            Map<String, Object> paramsMap = changeToMap(method, args);
            Map<String, String> responseHeader = new HashMap<>();
            String resutl = RestUtils.doExe(templateName, paramsMap,responseHeader);
            if (isBlank(resutl)) {
                return null;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType.isAssignableFrom(String.class)) {
                return resutl;
            }
            if(returnType.isAssignableFrom(ApiResponse.class)){
                ApiResponse api = new ApiResponse();
                api.setBody(resutl);
                api.setHeader(responseHeader);
                return resutl;
            }
            return JSON.parseObject(resutl, returnType);
        } catch (Throwable t) {
            logger.error(t);
            throw ExceptionUtil.unwrapThrowable(t);
        }
    }

    /**
     * 获取模板名
     * 
     * @param method
     *            method
     * @return 模板名字
     */
    private String getTemplateName(Method method) {
        String templateName = basedir + method.getDeclaringClass().getSimpleName() + "/" + method.getName() + ".json";
        logger.debug(templateName);
        return templateName;
    }

    /**
     * 参数转换为Map
     * 
     * @param method
     *            method
     * @param args
     *            参数
     * @return 结果
     */
    private Map<String, Object> changeToMap(Method method, Object[] args) {
        Map<String, Object> paramMap = new HashMap<>();
        if (args == null) {
            return paramMap;
        }
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if ((parameterAnnotations == null || parameterAnnotations.length < args.length) && args.length > 0) {
            throw new RuntimeException(method.getName() + "@Param is missing");
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Param param = (Param) parameterAnnotations[i][0];
            if (arg instanceof byte[]) {
                arg = Base64.encodeBase64String((byte[]) arg);
            }
            if (arg instanceof Enum) {
                arg = arg.toString();
            }
            paramMap.put(param.value(), arg);
        }
        return paramMap;
    }

    /**
     * 判空
     * 
     * @param dir
     *            目录
     * @return 结果
     */
    public static boolean isBlank(String dir) {
        if (dir == null || "".equals(dir.trim())) {
            return true;
        }
        return false;
    }

    /**
     * 结果
     * 
     * @param basedir
     *            目录
     */
    public IfaceProxy(String basedir) {
        super();
        logger.info("init RestProxy basedir:" + basedir);
        if (basedir != null) {
            if (!basedir.endsWith("/")) {
                basedir = basedir.concat("/");
            }
        }
        this.basedir = basedir;
        // 添加通用文件
        FkUtils.include(basedir);
    }
}
