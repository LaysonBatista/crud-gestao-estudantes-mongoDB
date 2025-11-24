

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

import conexion.MongoConnection;

public class MongoDatabaseSetup {

    public static void main(String[] args) {

        // Conecta no Mongo usando o DB_NAME que foi definido em MongoConnection
        MongoDatabase db = MongoConnection.getDatabase();

        // ===== COLEÇÃO ESTUDANTES =====
        MongoCollection<Document> estudantes = db.getCollection("estudantes");
        estudantes.createIndex(new Document("id_estudante", 1), new IndexOptions().unique(true));
        estudantes.createIndex(new Document("cpf", 1), new IndexOptions().unique(true));
        estudantes.createIndex(new Document("email", 1), new IndexOptions().unique(true));

        // ===== COLEÇÃO CURSOS =====
        MongoCollection<Document> cursos = db.getCollection("cursos");
        cursos.createIndex(new Document("id_curso", 1), new IndexOptions().unique(true));

        // ===== COLEÇÃO MATRICULAS =====
        MongoCollection<Document> matriculas = db.getCollection("matriculas");
        matriculas.createIndex(new Document("id_matricula", 1), new IndexOptions().unique(true));
        matriculas.createIndex(new Document("id_estudante", 1));
        matriculas.createIndex(new Document("id_curso", 1));
        // UNIQUE (id_estudante, id_curso) igual no MySQL:
        matriculas.createIndex(
                new Document("id_estudante", 1).append("id_curso", 1),
                new IndexOptions().unique(true));

        // ===== COLEÇÃO NOTAS =====
        MongoCollection<Document> notas = db.getCollection("notas");
        notas.createIndex(new Document("id_nota", 1), new IndexOptions().unique(true));
        notas.createIndex(new Document("id_matricula", 1));

        System.out.println("Índices criados/garantidos com sucesso no MongoDB!");

        MongoConnection.close();
    }
}
