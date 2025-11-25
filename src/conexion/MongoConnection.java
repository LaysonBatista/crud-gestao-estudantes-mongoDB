package conexion;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConnection {

    // URL padrão do servidor MongoDB local
    private static final String URI = "mongodb://mongoadmin:secret@localhost:27017/labdatabase?authSource=admin";

    // Nome do banco que você quer usar
    private static final String DB_NAME = "labdatabase";

    // Cliente único reutilizado
    private static MongoClient client = null;

    // Construtor privado para impedir new MongoConnection()
    private MongoConnection() {
    }

    public static MongoDatabase getDatabase() {
        if (client == null) {
            client = MongoClients.create(URI);
        }
        return client.getDatabase(DB_NAME);
    }

    public static void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
