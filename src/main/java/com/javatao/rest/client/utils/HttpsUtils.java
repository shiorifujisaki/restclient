package com.javatao.rest.client.utils;

import java.lang.reflect.Field;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.client.fluent.Executor;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

public class HttpsUtils {
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static SSLConnectionSocketFactory sslsf = null;
    private static PoolingHttpClientConnectionManager CONNMGR = null;
    private static SSLContextBuilder builder = null;
    static {
        try {
            builder = new SSLContextBuilder();
            // 全部信任 不做身份鉴定
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            });
            sslsf = new SSLConnectionSocketFactory(builder.build(), new String[] { "SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2" }, null,
                    NoopHostnameVerifier.INSTANCE);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
                    .register(HTTP, new PlainConnectionSocketFactory()).register(HTTPS, sslsf).build();
            CONNMGR = new PoolingHttpClientConnectionManager(registry);
            CONNMGR.setMaxTotal(200);
            CONNMGR.setDefaultMaxPerRoute(100);
            CONNMGR.setValidateAfterInactivity(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取 -CloseableHttpClient
     * 
     * @return CloseableHttpClient
     */
    public static CloseableHttpClient getHttpClient() {
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).setConnectionManager(CONNMGR).setConnectionManagerShared(true)
                .build();
        return httpClient;
    }

    public static void init() {
        Executor newInstance = Executor.newInstance();
        try {
            Field field = Executor.class.getDeclaredField("CLIENT");
            field.setAccessible(true);
            field.set(newInstance, getHttpClient());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
