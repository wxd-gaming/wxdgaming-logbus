package wxdgaming.logbus;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.logbus.http.HttpClientPool;
import wxdgaming.logbus.http.PostJson;
import wxdgaming.logbus.http.Response;
import wxdgaming.logbus.util.FileUtil;
import wxdgaming.logbus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * post执行器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-31 13:16
 **/
@Slf4j
public class PostRunnable implements Runnable {

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/HH");
    AtomicBoolean running = new AtomicBoolean(false);
    Path pathData;
    Path pathError;
    File file;

    public PostRunnable(Path path) {
        this.pathData = path.resolve("data");
        this.pathError = path.resolve("error");
        this.file = pathData.toFile();
    }

    @Override public void run() {
        try {
            if (!Files.exists(pathData)) return;
            List<Path> list1 = Files.walk(pathData, 99)
                    .filter(Files::isRegularFile)
                    .filter(v -> System.currentTimeMillis() - v.toFile().lastModified() > 2000)
                    .filter(v -> v.getFileName().toString().endsWith(".dat")).toList();

            if (list1.isEmpty()) return;
            CountDownLatch countDownLatch = new CountDownLatch(list1.size());
            list1.forEach(logFilePath -> {
                try {
                    int postCount = 0;
                    String url = null;
                    List<JSONObject> jsonData = null;
                    try {
                        List<String> list = Files.lines(logFilePath, StandardCharsets.UTF_8).toList();
                        postCount = Integer.parseInt(list.get(0));
                        url = list.get(1);
                        jsonData = JSON.parseArray(list.get(2), JSONObject.class);
                    } catch (Exception e) {
                        log.error("logbus walk error {}", pathData, e);
                        try {
                            Files.delete(logFilePath);
                        } catch (IOException ex) {
                            ex.printStackTrace(System.err);
                        }
                    }
                    if (url == null || url.isBlank()) {
                        return;
                    }
                    try {
                        JSONObject postData = new JSONObject();
                        postData.put("gameId", BootConfig.getIns().getAppId());
                        postData.put("token", BootConfig.getIns().getLogToken());
                        postData.put("data", jsonData);
                        Response<PostJson> request = new PostJson(HttpClientPool.getDefault(), BootConfig.getIns().getPortUrl() + "/" + url)
                                .setParams(postData.toJSONString())
                                .readTimeout(130000)
                                .retry(2)
                                .request();
                        if (request.responseCode() != 200 || JSONObject.parseObject(request.bodyString()).getInteger("code") != 1) {
                            log.info("logbus push {} fail {}", url, request.bodyString());
                        } else {
                            try {
                                Files.delete(logFilePath);
                            } catch (Exception ignored) {}
                        }
                    } catch (Exception e) {
                        try {
                            postCount++;
                            if (postCount > 10) {
                                Path errorPathString = pathError.resolve(sdf.format(new Date())).resolve(System.nanoTime() + "-" + StringUtils.randomString(4) + ".dat");
                                log.error("logbus push {} fail error log file {}", logFilePath, errorPathString, e);
                                FileUtil.writeString2File(errorPathString, "%s\n%s".formatted(url, JSON.toJSONString(jsonData)));
                                Files.delete(logFilePath);
                            } else {
                                FileUtil.writeString2File(logFilePath, "%s\n%s\n%s".formatted(postCount, url, JSON.toJSONString(jsonData)));
                            }
                        } catch (Exception ee) {
                            log.error("logbus push {} fail error log file ", logFilePath, ee);
                        }
                    }
                } finally {
                    countDownLatch.countDown();
                }
            });
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            log.error("logbus walk error {}", pathData, e);
        } finally {
            running.set(false);
        }
    }

}
