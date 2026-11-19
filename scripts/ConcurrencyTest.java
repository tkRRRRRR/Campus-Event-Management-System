import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 报名并发压测（无第三方依赖，JDK 8 直接编译运行）。
 *
 * 用法：
 *   java ConcurrencyTest <baseUrl> <activityId> <起始学生序号> <并发数>
 *   例：java ConcurrencyTest http://127.0.0.1:8080 12 1 50
 *
 * 学生账号规则：序号 n -> 用户名 2023 + 6 位（200+n），密码统一 123456
 *
 * 输出：
 *   成功 / 名额已满 / 其他失败 的数量，
 *   以及压测后活动详情里的 signedCount 与 joinedCount（两者应相等且等于名额上限）。
 */
public class ConcurrencyTest {

    private static final String PASSWORD = "123456";

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("用法: ConcurrencyTest <baseUrl> <activityId> <起始序号> <并发数>");
            return;
        }
        String base = args[0];
        int activityId = Integer.parseInt(args[1]);
        int startIndex = Integer.parseInt(args[2]);
        int count = Integer.parseInt(args[3]);

        System.out.println("目标: " + base + "/api/registration/" + activityId);
        System.out.println("并发: " + count + " 个用户");

        // 1. 先串行登录拿 token，避免把登录耗时算进并发窗口
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String username = "2023" + String.format("%06d", 200 + startIndex + i);
            String token = login(base, username);
            if (token == null) {
                System.out.println("登录失败: " + username);
                return;
            }
            tokens.add(token);
        }
        System.out.println("登录完成，开始并发报名 ...\n");

        // 2. 同时起跑
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(count);
        Map<String, Integer> results = new ConcurrentHashMap<>();

        for (String token : tokens) {
            new Thread(() -> {
                try {
                    ready.countDown();
                    start.await();
                    String body = post(base + "/api/registration/" + activityId, token);
                    results.merge(classify(body), 1, Integer::sum);
                } catch (Exception e) {
                    results.merge("请求异常", 1, Integer::sum);
                } finally {
                    done.countDown();
                }
            }).start();
        }

        ready.await();
        long begin = System.currentTimeMillis();
        start.countDown();
        done.await();
        long cost = System.currentTimeMillis() - begin;

        int success = results.getOrDefault("成功", 0);
        int soldOut = results.getOrDefault("名额已满", 0);
        int other = count - success - soldOut;

        System.out.println("===== 结果 =====");
        System.out.println("耗时        : " + cost + " ms");
        System.out.println("成功        : " + success);
        System.out.println("名额已满    : " + soldOut);
        System.out.println("其他失败    : " + other);
        for (Map.Entry<String, Integer> e : results.entrySet()) {
            if (!"成功".equals(e.getKey()) && !"名额已满".equals(e.getKey())) {
                System.out.println("  - " + e.getKey() + " x" + e.getValue());
            }
        }

        // 3. 用第一个用户的 token 查详情，核对计数
        String detail = get(base + "/api/activity/" + activityId, tokens.get(0));
        int signed = extractInt(detail, "signedCount");
        int joined = extractInt(detail, "joinedCount");
        int capacity = extractInt(detail, "capacity");

        System.out.println("\n===== 数据核对 =====");
        System.out.println("名额上限    : " + capacity);
        System.out.println("signedCount : " + signed);
        System.out.println("joinedCount : " + joined);

        boolean ok = signed == capacity && joined == capacity && success == capacity;
        System.out.println("\n判定: " + (ok ? "未超卖 ✅" : "出现异常 ❌"));

        if (signed == joined) {
            System.out.println("（signedCount 与 joinedCount 相等，冗余计数与明细一致）");
        } else {
            System.out.println("（signedCount 与 joinedCount 不相等！冗余计数已丢失更新，差 "
                    + (joined - signed) + " 条）");
        }
        if (joined > capacity) {
            System.out.println("（报名明细 " + joined + " 条超过名额上限 " + capacity
                    + "，实际超卖 " + (joined - capacity) + " 人）");
        }
    }

    /* ---------------- HTTP ---------------- */

    private static String login(String base, String username) {
        try {
            String body = "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}";
            String resp = postJson(base + "/api/auth/login", body);
            String token = extract(resp, "\"token\":\"([^\"]+)\"");
            return token;
        } catch (Exception e) {
            return null;
        }
    }

    private static String post(String url, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(new byte[0]);
        }
        return read(conn);
    }

    private static String postJson(String url, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return read(conn);
    }

    private static String get(String url, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        return read(conn);
    }

    private static String read(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) != -1) {
            bos.write(buf, 0, len);
        }
        in.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    /* ---------------- 解析 ---------------- */

    private static String classify(String body) {
        String msg = extract(body, "\"msg\":\"([^\"]*)\"");
        if (msg == null) {
            return "响应无法解析";
        }
        if (msg.contains("报名成功")) {
            return "成功";
        }
        if (msg.contains("名额已满")) {
            return "名额已满";
        }
        return msg;
    }

    private static int extractInt(String json, String key) {
        String v = extract(json, "\"" + key + "\":(\\d+)");
        return v == null ? -1 : Integer.parseInt(v);
    }

    private static String extract(String text, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : null;
    }
}
