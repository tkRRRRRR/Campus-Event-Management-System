import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库初始化小工具：把 db/ 下的 SQL 按顺序执行到指定的 MySQL 库。
 * 不依赖 mysql 命令行客户端，只需要 mysql-connector-java。
 *
 * 编译：javac -encoding UTF-8 -cp mysql-connector-java-8.0.33.jar -d out DbInit.java
 * 运行：java -cp "mysql-connector-java-8.0.33.jar;out" DbInit 主机 端口 库名 账号 密码 [--schema-only]
 */
public class DbInit {

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("用法: DbInit <host> <port> <database> <user> <password> [--schema-only]");
            return;
        }
        String host = args[0];
        String port = args[1];
        String database = args[2];
        String user = args[3];
        String password = args[4];
        boolean schemaOnly = args.length > 5 && "--schema-only".equals(args[5]);

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?characterEncoding=utf8"
                + "&useSSL=true&requireSSL=false&verifyServerCertificate=false"
                + "&serverTimezone=Asia/Shanghai"
                + "&allowPublicKeyRetrieval=true"
                + "&connectTimeout=20000&socketTimeout=120000";

        System.out.println("连接: " + host + ":" + port + "/" + database + " (user=" + user + ")");
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("连接成功，服务端版本: " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("驱动版本: " + conn.getMetaData().getDriverVersion());

            List<String> files = new ArrayList<>();
            files.add("db/schema.sql");
            files.add("db/indexes.sql");
            if (!schemaOnly) {
                files.add("db/data.sql");
            }

            for (String file : files) {
                String content = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
                List<String> statements = splitStatements(content);
                int ok = 0;
                int fail = 0;
                for (String sql : statements) {
                    try (Statement st = conn.createStatement()) {
                        st.execute(sql);
                        ok++;
                    } catch (SQLException e) {
                        fail++;
                        String preview = sql.replaceAll("\\s+", " ");
                        if (preview.length() > 70) {
                            preview = preview.substring(0, 70);
                        }
                        System.out.println("  [FAIL] " + e.getMessage() + "\n         SQL: " + preview);
                    }
                }
                System.out.println(file + " => 成功 " + ok + " 条，失败 " + fail + " 条");
            }
            System.out.println("初始化结束");
        }
    }

    /**
     * 按分号切分，去掉整行的 -- 注释。
     * 本项目 SQL 里没有存储过程，所以简单切分足够。
     */
    private static List<String> splitStatements(String content) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        List<String> result = new ArrayList<>();
        for (String part : cleaned.toString().split(";")) {
            String sql = part.trim();
            if (!sql.isEmpty()) {
                result.add(sql);
            }
        }
        return result;
    }
}
