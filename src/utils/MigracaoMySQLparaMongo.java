package utils;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;

import conexion.Conexao;
import conexion.MongoConnection;

public class MigracaoMySQLparaMongo {

    public static void main(String[] args) {

        System.out.println("== MIGRAÇÃO MySQL -> MongoDB (labdatabase) ==");

        // Conecta no Mongo (labdatabase) e no MySQL (labdatabase da C2)
        MongoDatabase db = MongoConnection.getDatabase();

        try (Connection cn = Conexao.getConnection()) {

            migrarEstudantes(cn, db);
            migrarCursos(cn, db);
            migrarMatriculas(cn, db);
            migrarNotas(cn, db);

            System.out.println("Migração concluída.");

        } catch (SQLException e) {
            System.err.println("Erro de SQL durante a migração: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro geral durante a migração: " + e.getMessage());
        } finally {
            MongoConnection.close();
        }
    }

    // ==================== ESTUDANTES ====================
    private static void migrarEstudantes(Connection cn, MongoDatabase db) throws SQLException {
        System.out.println("\n-- Migrando ESTUDANTES --");

        MongoCollection<Document> col = db.getCollection("estudantes");

        String sql = "SELECT id_estudante, nome, data_nascimento, cpf, email FROM ESTUDANTES";

        int inseridos = 0;
        int pulados = 0;

        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_estudante");
                String nome = rs.getString("nome");
                Date dataNascSql = rs.getDate("data_nascimento");
                String dataNasc = (dataNascSql != null) ? dataNascSql.toLocalDate().toString() : null;
                String cpf = rs.getString("cpf");
                String email = rs.getString("email");

                // verifica se já existe no Mongo
                Document existente = col.find(eq("id_estudante", id)).first();
                if (existente != null) {
                    pulados++;
                    continue;
                }

                Document doc = new Document()
                        .append("id_estudante", id)
                        .append("nome", nome)
                        .append("data_nascimento", dataNasc)
                        .append("cpf", cpf)
                        .append("email", email);

                col.insertOne(doc);
                inseridos++;
            }
        }

        System.out.printf("Estudantes inseridos: %d | já existentes pulados: %d%n", inseridos, pulados);
    }

    // ==================== CURSOS ====================
    private static void migrarCursos(Connection cn, MongoDatabase db) throws SQLException {
        System.out.println("\n-- Migrando CURSOS --");

        MongoCollection<Document> col = db.getCollection("cursos");

        String sql = "SELECT id_curso, nome_curso, carga_horaria FROM CURSOS";

        int inseridos = 0;
        int pulados = 0;

        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_curso");
                String nome = rs.getString("nome_curso");
                int ch = rs.getInt("carga_horaria");

                Document existente = col.find(eq("id_curso", id)).first();
                if (existente != null) {
                    pulados++;
                    continue;
                }

                Document doc = new Document()
                        .append("id_curso", id)
                        .append("nome_curso", nome)
                        .append("carga_horaria", ch);

                col.insertOne(doc);
                inseridos++;
            }
        }

        System.out.printf("Cursos inseridos: %d | já existentes pulados: %d%n", inseridos, pulados);
    }

    // ==================== MATRICULAS ====================
    private static void migrarMatriculas(Connection cn, MongoDatabase db) throws SQLException {
        System.out.println("\n-- Migrando MATRICULAS --");

        MongoCollection<Document> col = db.getCollection("matriculas");

        String sql = "SELECT id_matricula, data_matricula, status_matricula, id_estudante, id_curso FROM MATRICULAS";

        int inseridos = 0;
        int pulados = 0;

        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_matricula");
                Date dataMatSql = rs.getDate("data_matricula");
                String dataMat = (dataMatSql != null) ? dataMatSql.toLocalDate().toString() : null;
                String status = rs.getString("status_matricula");
                int idEst = rs.getInt("id_estudante");
                int idCurso = rs.getInt("id_curso");

                Document existente = col.find(eq("id_matricula", id)).first();
                if (existente != null) {
                    pulados++;
                    continue;
                }

                Document doc = new Document()
                        .append("id_matricula", id)
                        .append("data_matricula", dataMat)
                        .append("status_matricula", status)
                        .append("id_estudante", idEst)
                        .append("id_curso", idCurso);

                col.insertOne(doc);
                inseridos++;
            }
        }

        System.out.printf("Matriculas inseridas: %d | já existentes puladas: %d%n", inseridos, pulados);
    }

    // ==================== NOTAS ====================
    private static void migrarNotas(Connection cn, MongoDatabase db) throws SQLException {
        System.out.println("\n-- Migrando NOTAS --");

        MongoCollection<Document> col = db.getCollection("notas");

        String sql = "SELECT id_nota, nota_estudante, semestre, id_matricula FROM NOTAS";

        int inseridos = 0;
        int pulados = 0;

        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_nota");
                double nota = rs.getDouble("nota_estudante");
                String semestre = rs.getString("semestre");
                int idMat = rs.getInt("id_matricula");

                Document existente = col.find(eq("id_nota", id)).first();
                if (existente != null) {
                    pulados++;
                    continue;
                }

                Document doc = new Document()
                        .append("id_nota", id)
                        .append("nota_estudante", nota)
                        .append("semestre", semestre)
                        .append("id_matricula", idMat);

                col.insertOne(doc);
                inseridos++;
            }
        }

        System.out.printf("Notas inseridas: %d | já existentes puladas: %d%n", inseridos, pulados);
    }
}

