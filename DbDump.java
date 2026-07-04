import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class DbDump {
    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://ep-lively-moon-ajv7wquu-pooler.c-3.us-east-2.aws.neon.tech/neondb?sslmode=require&channelBinding=require";
            Properties props = new Properties();
            props.setProperty("user", "neondb_owner");
            props.setProperty("password", "npg_wqy0ivcI6MeG");
            try (Connection conn = DriverManager.getConnection(url, props);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM ticket_base_prices_v2")) {
                while (rs.next()) {
                    System.out.println(rs.getLong("id") + " | " + rs.getString("name") + " | " + rs.getString("ticket_type") + " | " + rs.getString("formato"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
