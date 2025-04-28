package wxdgaming.logbus.http;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import wxdgaming.logbus.util.GzipUtil;

import java.io.IOException;

@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public class PostJson extends HttpBase<PostJson> {

    private ContentType contentType = HttpConst.APPLICATION_JSON;
    private String params = "";

    public PostJson(HttpClientPool httpClientPool, String uriPath) {
        super(httpClientPool, uriPath);
    }

    @Override public void request0() throws IOException {
        HttpPost httpRequest = createPost();
        if (null != params) {
            byte[] bytes = params.getBytes(contentType.getCharset());
            if (bytes.length > 512) {
                // 设置请求头，告知服务器请求内容使用 Gzip 压缩
                httpRequest.setHeader("Content-Encoding", "gzip");
                bytes = GzipUtil.gzip(bytes);
            }
            ByteArrayEntity requestEntity = new ByteArrayEntity(bytes, contentType);
            httpRequest.setEntity(requestEntity);
            if (log.isDebugEnabled()) {
                log.debug("send url={}\n{}", url(), params);
            }
        }
        CloseableHttpClient closeableHttpClient = httpClientPool.getCloseableHttpClient();
        closeableHttpClient.execute(httpRequest, classicHttpResponse -> {
            response.httpResponse = classicHttpResponse;
            response.cookieStore = httpClientPool.getCookieStore().getCookies();
            String header = response.getHeader(HttpHeadNameType.Content_Encoding.getValue());
            response.bodys = EntityUtils.toByteArray(classicHttpResponse.getEntity());
            if (header != null && header.toLowerCase().contains("gzip")) {
                response.bodys = GzipUtil.unGZip(response.bodys);
            }
            return null;
        });
    }

    @Override public PostJson addHeader(String headerKey, String HeaderValue) {
        super.addHeader(headerKey, HeaderValue);
        return this;
    }

    public PostJson setParams(String params) {
        this.params = params;
        return this;
    }

}
