import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class DbDump {
    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            String url = System.getenv("DB_URL");
            Properties props = new Properties();
            props.setProperty("user", System.getenv("DB_USERNAME"));
            props.setProperty("password", System.getenv("DB_PASSWORD"));
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
