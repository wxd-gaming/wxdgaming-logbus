package wxdgaming.logbus.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NoHttpResponseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * http构建协议
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2023-04-28 12:44
 **/
@Slf4j
public abstract class HttpBase<H extends HttpBase> {

    protected HttpClientPool httpClientPool;
    protected long logTime = 200;
    protected long waringTime = 1200;
    protected int connectionRequestTimeout;
    protected int readTimeout;
    protected int retry = 1;
    protected String uriPath;
    protected final Map<String, String> reqHeaderMap = new LinkedHashMap<>();

    protected final Response<H> response;
    protected StackTraceElement[] stackTraceElements;

    public HttpBase(HttpClientPool httpClientPool, String uriPath) {
        this.httpClientPool = httpClientPool;
        this.uriPath = uriPath;
        connectionRequestTimeout = httpClientPool.getClientConfig().getConnectionRequestTimeout();
        readTimeout = httpClientPool.getClientConfig().getReadTimeout();
        header("user-agent", "wxd-gaming jdk 21");
        response = new Response(this, uriPath);
    }

    public void close() {
        try {
            if (this.response.httpResponse != null) this.response.httpResponse.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final Response<H> request() {
        Exception exception = null;
        try {
            for (int k = 0; k < retry; k++) {
                try {
                    request0();
                    return this.response;
                } catch (NoHttpResponseException
                         | SocketTimeoutException
                         | SocketException e) {
                    exception = e;
                    if (k > 0) {
                        log.error("请求异常，重试 {}", k, e);
                    }
                } catch (IllegalStateException | InterruptedIOException e) {
                    exception = e;
                    /*todo 因为意外链接终止了 重新构建 */
                    String string = e.toString();
                    if (string.contains("shut") && string.contains("down")) {
                        log.error("连接池可能意外关闭了重新构建，等待重试 {} {}", k, string);
                    } else {
                        log.error("连接池可能意外关闭了重新构建，等待重试 {}", k, e);
                    }
                } catch (Exception e) {
                    exception = e;
                    log.error("请求异常，重试 {}", k, e);
                }
            }
        } finally {
            close();
        }
        throw new RuntimeException(exception);
    }

    protected abstract void request0() throws IOException;

    protected HttpGet createGet() {
        HttpGet post = new HttpGet(uriPath);
        writeHeader(post);
        return post;
    }

    protected HttpPost createPost() {
        HttpPost post = new HttpPost(uriPath);
        writeHeader(post);
        return post;
    }

    protected void writeHeader(HttpUriRequestBase httpRequestBase) {

        // 初始化请求超时控制参数
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(connectionRequestTimeout, TimeUnit.MILLISECONDS) // 从线程池中获取线程超时时间
                .setResponseTimeout(readTimeout, TimeUnit.MILLISECONDS) // 设置数据超时时间
                .build();

        /*超时设置*/
        httpRequestBase.setConfig(requestConfig);

        for (Map.Entry<String, String> entry : reqHeaderMap.entrySet()) {
            httpRequestBase.setHeader(entry.getKey().toString(), entry.getValue());
        }

        /*告诉服务器我支持gzip*/
        httpRequestBase.setHeader(HttpHeaders.ACCEPT_ENCODING, HttpHeadValueType.Gzip.getValue());

        // 防止被当成攻击添加的
        httpRequestBase.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.2; Win64; x64) wxd");

    }

    /**
     * 设置参数头
     *
     * @param headerKey   采用这个 HttpHeaderNames
     * @param HeaderValue
     * @return
     */
    public H addHeader(String headerKey, String HeaderValue) {
        this.reqHeaderMap.put(headerKey, HeaderValue);
        return (H) this;
    }

    protected byte[] readBytes(HttpEntity http) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            http.writeTo(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 同时设置连接超时和读取超时时间 */
    public H logTime(int time) {
        this.logTime = time;
        return (H) this;
    }

    /** 同时设置连接超时和读取超时时间 */
    public H waringTime(int time) {
        this.waringTime = time;
        return (H) this;
    }

    /** 从连接池获取连接超时时间 ms */
    public H connectionRequestTimeout(int timeout) {
        this.connectionRequestTimeout = timeout;
        return (H) this;
    }

    /** 连接成功读取数据超时 ms */
    public H readTimeout(int timeout) {
        this.readTimeout = timeout;
        return (H) this;
    }

    /** 设置重试次数 */
    public H retry(int retry) {
        if (retry < 1) throw new RuntimeException("重试次数最少是1");
        this.retry = retry;
        return (H) this;
    }

    public H header(String headerKey, String HeaderValue) {
        this.reqHeaderMap.put(headerKey, HeaderValue);
        return (H) this;
    }

    public String url() {
        return response.uriPath;
    }

    public String getPostText() {
        return response.getPostText();
    }

    @Override public String toString() {
        return String.valueOf(this.response);
    }

}
