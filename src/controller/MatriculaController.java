package controller;

import model.Matricula;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import conexion.MongoConnection;

public class MatriculaController {
    // Coleção
    private final MongoCollection<Document> matriculas;
    private final MongoCollection<Document> estudantes;
    private final MongoCollection<Document> cursos;
    private final MongoCollection<Document> notas;

    public MatriculaController() {
        MongoDatabase db = MongoConnection.getDatabase();
        this.matriculas = db.getCollection("matriculas");
        this.estudantes = db.getCollection("estudantes");
        this.cursos = db.getCollection("cursos");
        this.notas = db.getCollection("notas");
    }

    // util para gerar próximo ID inteiro (baseado no maior existente)
    private int getNextId(MongoCollection<Document> col, String field) {
        Document last = col.find()
                .sort(Sorts.descending(field))
                .first();

        if (last == null || last.get(field) == null)
            return 1;

        Object v = last.get(field);
        if (v instanceof Number) {
            return ((Number) v).intValue() + 1;
        }
        return 1;
    }

    // normaliza status
    private String normalizaStatus(String s) {
        if (s == null)
            return null;
        String x = s.trim().toLowerCase();
        if (x.equals("ativo") || x.equals("ativa"))
            return "Ativo";
        if (x.equals("inativo") || x.equals("inativa"))
            return "Inativo";
        return null;
    }

    // INSERIR
    public void inserir(Scanner in) {
        System.out.print("ID estudante: ");
        int idE = Integer.parseInt(in.nextLine());

        System.out.print("ID curso: ");
        int idC = Integer.parseInt(in.nextLine());

        System.out.print("Data matrícula (AAAA-MM-DD): ");
        LocalDate d = LocalDate.parse(in.nextLine());

        System.out.print("Status (Ativo/Inativo): ");
        String status = normalizaStatus(in.nextLine());

        if (status == null) {
            System.err.println("Status inválido. Use 'Ativo' ou 'Inativo'.");
            return;
        }

        try {
            // valida se estudante existe
            Document est = estudantes.find(Filters.eq("id_estudante", idE)).first();
            if (est == null) {
                System.err.println("Estudante não encontrado (FK inválida).");
                return;
            }

            // valida se curso existe
            Document cur = cursos.find(Filters.eq("id_curso", idC)).first();
            if (cur == null) {
                System.err.println("Curso não encontrado (FK inválida).");
                return;
            }

            // verifica duplicidade (id_estudante, id_curso)
            long dupCount = matriculas.countDocuments(
                    Filters.and(
                            Filters.eq("id_estudante", idE),
                            Filters.eq("id_curso", idC)));

            if (dupCount > 0) {
                System.err.println("Matrícula duplicada: (id_estudante, id_curso) já existe.");
                return;
            }

            int idMatricula = getNextId(matriculas, "id_matricula");

            Document doc = new Document()
                    .append("id_matricula", idMatricula)
                    .append("id_estudante", idE)
                    .append("id_curso", idC)
                    .append("data_matricula", d.toString()) // yyyy-MM-dd
                    .append("status_matricula", status);

            matriculas.insertOne(doc);
            System.out.println("Matrícula inserida!");

        } catch (Exception e) {
            System.err.println("Erro ao inserir matrícula no Mongo: " + e.getMessage());
        }
    }

    // REMOVER
    public void remover(Scanner in) {
        listar(); // mostra para escolher

        System.out.print("ID matrícula: ");
        int id = Integer.parseInt(in.nextLine());

        try {
            // verifica se matrícula existe
            Document mat = matriculas.find(Filters.eq("id_matricula", id)).first();
            if (mat == null) {
                System.out.println("Matrícula não encontrada.");
                return;
            }

            // 1) remover notas ligadas a essa matrícula (equivalente ao ON DELETE CASCADE)
            long notasRemovidas = notas.deleteMany(Filters.eq("id_matricula", id)).getDeletedCount();

            // 2) remover a matrícula
            long matsRemovidas = matriculas.deleteOne(Filters.eq("id_matricula", id)).getDeletedCount();

            System.out.printf("Removidas %d nota(s).%n", notasRemovidas);
            System.out.println(matsRemovidas > 0 ? "Matrícula removida!" : "Nada removido.");

        } catch (Exception e) {
            System.err.println("Erro ao remover matrícula no Mongo: " + e.getMessage());
        }
    }

    // LISTAR
    public void listar() {
        try (MongoCursor<Document> cursor = matriculas.find()
                .sort(Sorts.ascending("id_matricula"))
                .iterator()) {

            System.out.println("\n-- MATRÍCULAS --");
            System.out.printf("%-4s | %-20s | %-25s | %-10s | %-12s%n",
                    "ID", "Estudante", "Curso", "Status", "Data");
            System.out.println("-----+----------------------+---------------------------+------------+--------------");

            boolean vazio = true;

            while (cursor.hasNext()) {
                vazio = false;
                Document m = cursor.next();

                int idMat = ((Number) m.get("id_matricula")).intValue();
                int idE = ((Number) m.get("id_estudante")).intValue();
                int idC = ((Number) m.get("id_curso")).intValue();
                String status = m.getString("status_matricula");
                String dataStr = m.getString("data_matricula");

                String nomeEstudante = "(desconhecido)";
                Document est = estudantes.find(Filters.eq("id_estudante", idE)).first();
                if (est != null) {
                    nomeEstudante = est.getString("nome");
                }

                String nomeCurso = "(desconhecido)";
                Document cur = cursos.find(Filters.eq("id_curso", idC)).first();
                if (cur != null) {
                    nomeCurso = cur.getString("nome_curso");
                }

                String dataMat = (dataStr != null && !dataStr.isBlank()) ? dataStr : "";

                System.out.printf("%-4s | %-20s | %-25s | %-10s | %-12s%n",
                        "#" + idMat, nomeEstudante, nomeCurso, status, dataMat);
            }

            if (vazio) {
                System.out.println("(Nenhuma matrícula cadastrada)");
            }

        } catch (Exception e) {
            System.err.println("Erro ao listar matrículas no Mongo: " + e.getMessage());
        }
    }

    //
    public List<Matricula> findAllComoObjetos() {
        List<Matricula> lista = new ArrayList<>();

        try (MongoCursor<Document> cursor = matriculas.find()
                .sort(Sorts.ascending("id_matricula"))
                .iterator()) {

            while (cursor.hasNext()) {
                Document m = cursor.next();

                int idMat = ((Number) m.get("id_matricula")).intValue();
                int idE = ((Number) m.get("id_estudante")).intValue();
                int idC = ((Number) m.get("id_curso")).intValue();
                String status = m.getString("status_matricula");
                String dataStr = m.getString("data_matricula");
                LocalDate dataMat = null;
                if (dataStr != null && !dataStr.isBlank()) {
                    dataMat = LocalDate.parse(dataStr);
                }

                Matricula mat = new Matricula(
                        idMat,
                        dataMat,
                        status,
                        idE,
                        idC);

                lista.add(mat);
            }

        } catch (Exception e) {
            System.err.println("Erro ao consultar matrículas no Mongo: " + e.getMessage());
        }

        return lista;
    }

}
