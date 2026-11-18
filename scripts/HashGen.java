import cn.hutool.crypto.digest.BCrypt;

/**
 * 开发期小工具：生成 BCrypt 密码哈希，用于初始化演示数据。
 * 只在本地跑，不参与打包。
 *
 * 用法：java -cp "hutool.jar;out" HashGen 123456
 */
public class HashGen {

    public static void main(String[] args) {
        String raw = args.length > 0 ? args[0] : "123456";
        String hash = BCrypt.hashpw(raw);
        System.out.println("raw  = " + raw);
        System.out.println("hash = " + hash);
        System.out.println("verify = " + BCrypt.checkpw(raw, hash));
    }
}
