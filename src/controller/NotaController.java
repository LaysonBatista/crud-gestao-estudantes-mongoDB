package controller;

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

public class NotaController {

    // Coleção
    private final MongoCollection<Document> notas;
    private final MongoCollection<Document> matriculas;
    private final MongoCollection<Document> estudantes;
    private final MongoCollection<Document> cursos;

    public NotaController() {
        MongoDatabase db = MongoConnection.getDatabase();
        this.notas = db.getCollection("notas");
        this.matriculas = db.getCollection("matriculas");
        this.estudantes = db.getCollection("estudantes");
        this.cursos = db.getCollection("cursos");
    }

    // ========= util para gerar próximo ID inteiro =========
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

    // leitura de double segura
    private double lerDouble(Scanner in, String label) {
        while (true) {
            System.out.print(label);
            String s = in.nextLine().replace(",", ".").trim();
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido. Digite um número (ex: 8.5).");
            }
        }
    }

    // ================= METÓDOS

    // INSERIR NOTA (pedindo id_matricula)
    public void inserir(Scanner in) {
        System.out.print("ID matrícula: ");
        int idMat = Integer.parseInt(in.nextLine());

        // valida matrícula
        Document mat = matriculas.find(Filters.eq("id_matricula", idMat)).first();
        if (mat == null) {
            System.err.println("Matrícula não encontrada.");
            return;
        }

        double nota = lerDouble(in, "Nota do estudante: ");
        System.out.print("Semestre (ex: 2024/1): ");
        String semestre = in.nextLine();

        try {
            int idNota = getNextId(notas, "id_nota");

            Document doc = new Document()
                    .append("id_nota", idNota)
                    .append("id_matricula", idMat)
                    .append("nota_estudante", nota)
                    .append("semestre", semestre);

            notas.insertOne(doc);
            System.out.println("Nota inserida! (id_nota = " + idNota + ")");
        } catch (Exception e) {
            System.err.println("Erro ao inserir nota no Mongo: " + e.getMessage());
        }
    }

    // ATUALIZAR NOTA
    public void atualizar(Scanner in) {
        listar();

        System.out.print("ID da nota: ");
        int idNota = Integer.parseInt(in.nextLine());

        Document notaDoc = notas.find(Filters.eq("id_nota", idNota)).first();
        if (notaDoc == null) {
            System.out.println("Nota não encontrada.");
            return;
        }

        double novaNota = lerDouble(in, "Nova nota: ");
        System.out.print("Novo semestre: ");
        String novoSemestre = in.nextLine();

        try {
            Document filtro = new Document("id_nota", idNota);
            Document novosDados = new Document()
                    .append("nota_estudante", novaNota)
                    .append("semestre", novoSemestre);

            Document update = new Document("$set", novosDados);

            long modificados = notas.updateOne(filtro, update).getModifiedCount();
            System.out.println(modificados > 0 ? "Nota atualizada!" : "Nada atualizado.");
        } catch (Exception e) {
            System.err.println("Erro ao atualizar nota no Mongo: " + e.getMessage());
        }
    }

    // REMOVER NOTA
    public void remover(Scanner in) {
        listar();

        System.out.print("ID da nota: ");
        int id = Integer.parseInt(in.nextLine());

        try {
            long removidos = notas.deleteOne(Filters.eq("id_nota", id)).getDeletedCount();
            System.out.println(removidos > 0 ? "Nota removida!" : "Nada removido.");
        } catch (Exception e) {
            System.err.println("Erro ao remover nota no Mongo: " + e.getMessage());
        }
    }

    // LISTAR TODAS AS NOTAS
    public void listar() {
        System.out.println("\n-- NOTAS --");
        System.out.printf("%-4s | %-20s | %-25s | %-10s | %-8s%n",
                "ID", "Estudante", "Curso", "Semestre", "Nota");
        System.out.println("-----+----------------------+---------------------------+------------+----------");

        boolean vazio = true;

        try (MongoCursor<Document> cursor = notas.find()
                .sort(Sorts.ascending("id_nota"))
                .iterator()) {

            while (cursor.hasNext()) {
                vazio = false;
                Document n = cursor.next();

                int idNota = ((Number) n.get("id_nota")).intValue();
                double nota = ((Number) n.get("nota_estudante")).doubleValue();
                String semestre = n.getString("semestre");
                int idMat = ((Number) n.get("id_matricula")).intValue();

                Document mat = matriculas.find(Filters.eq("id_matricula", idMat)).first();

                String aluno = "(desconhecido)";
                String curso = "(desconhecido)";

                if (mat != null) {
                    int idEstudante = ((Number) mat.get("id_estudante")).intValue();
                    int idCurso = ((Number) mat.get("id_curso")).intValue();

                    Document est = estudantes.find(Filters.eq("id_estudante", idEstudante)).first();
                    Document cur = cursos.find(Filters.eq("id_curso", idCurso)).first();

                    if (est != null)
                        aluno = est.getString("nome");
                    if (cur != null)
                        curso = cur.getString("nome_curso");
                }

                System.out.printf("%-4s | %-20s | %-25s | %-10s | %-8.2f%n",
                        "#" + idNota, aluno, curso, semestre, nota);
            }

        } catch (Exception e) {
            System.err.println("Erro ao listar notas no Mongo: " + e.getMessage());
            return;
        }

        if (vazio) {
            System.out.println("(Nenhuma nota cadastrada)");
        }
    }

    // LISTAR POR MATRÍCULA
    public void listarPorMatricula(Scanner in) {
        System.out.print("ID matrícula: ");
        int idMat = Integer.parseInt(in.nextLine());

        List<String> lista = new ArrayList<>();

        try (MongoCursor<Document> cursor = notas.find(Filters.eq("id_matricula", idMat))
                .sort(Sorts.ascending("id_nota"))
                .iterator()) {

            while (cursor.hasNext()) {
                Document n = cursor.next();
                int idNota = ((Number) n.get("id_nota")).intValue();
                double nota = ((Number) n.get("nota_estudante")).doubleValue();
                String semestre = n.getString("semestre");

                lista.add(String.format("#%d | id_matricula=%d | %s | nota: %.2f",
                        idNota, idMat, semestre, nota));
            }

        } catch (Exception e) {
            System.err.println("Erro ao listar notas da matrícula no Mongo: " + e.getMessage());
            return;
        }

        if (lista.isEmpty()) {
            System.out.println("(Nenhuma nota para essa matrícula)");
        } else {
            lista.forEach(System.out::println);
        }
    }

    // LISTAR POR CURSO + SEMESTRE
    public void listarPorCursoSemestre(Scanner in) {
        System.out.print("ID curso: ");
        int idCurso = Integer.parseInt(in.nextLine());

        System.out.print("Semestre (ex: 2024/1): ");
        String semestre = in.nextLine();

        List<String> lista = new ArrayList<>();

        try (MongoCursor<Document> cursor = notas.find(Filters.eq("semestre", semestre))
                .sort(Sorts.ascending("id_nota"))
                .iterator()) {

            while (cursor.hasNext()) {
                Document n = cursor.next();
                int idNota = ((Number) n.get("id_nota")).intValue();
                double nota = ((Number) n.get("nota_estudante")).doubleValue();
                int idMat = ((Number) n.get("id_matricula")).intValue();

                Document mat = matriculas.find(Filters.eq("id_matricula", idMat)).first();
                if (mat == null)
                    continue;

                int idCursoDaMat = ((Number) mat.get("id_curso")).intValue();
                if (idCursoDaMat != idCurso)
                    continue; // nota de outro curso

                int idEstudante = ((Number) mat.get("id_estudante")).intValue();

                Document est = estudantes.find(Filters.eq("id_estudante", idEstudante)).first();
                String nomeAluno = est != null ? est.getString("nome") : "(desconhecido)";

                lista.add(String.format("#%d | %s | semestre %s | nota: %.2f",
                        idNota, nomeAluno, semestre, nota));
            }

        } catch (Exception e) {
            System.err.println("Erro ao listar notas por curso/semestre no Mongo: " + e.getMessage());
            return;
        }

        if (lista.isEmpty()) {
            System.out.println("(Nenhuma nota encontrada para esse curso/semestre)");
        } else {
            lista.forEach(System.out::println);
        }
    }

    // LISTAR NOTAS POR ESTUDANTE
    public void listarPorEstudantePorId(Scanner in) {
        System.out.print("ID estudante: ");
        int idEst = Integer.parseInt(in.nextLine());

        // Buscar o estudante para pegar o nome
        Document est = estudantes.find(Filters.eq("id_estudante", idEst)).first();
        if (est == null) {
            System.out.println("Estudante não encontrado.");
            return;
        }
        String nomeEstudante = est.getString("nome");

        List<String> lista = new ArrayList<>();

        // 1) pegar matrículas do estudante
        List<Integer> idsMat = new ArrayList<>();
        try (MongoCursor<Document> cursor = matriculas.find(Filters.eq("id_estudante", idEst)).iterator()) {
            while (cursor.hasNext()) {
                Document m = cursor.next();
                idsMat.add(((Number) m.get("id_matricula")).intValue());
            }
        }

        if (idsMat.isEmpty()) {
            System.out.println("Estudante não possui matrículas.");
            return;
        }

        // 2) listar notas dessas matrículas
        try (MongoCursor<Document> cursor = notas.find(Filters.in("id_matricula", idsMat))
                .sort(Sorts.ascending("id_nota"))
                .iterator()) {

            while (cursor.hasNext()) {
                Document n = cursor.next();
                int idNota = ((Number) n.get("id_nota")).intValue();
                double nota = ((Number) n.get("nota_estudante")).doubleValue();
                String semestre = n.getString("semestre");
                int idMat = ((Number) n.get("id_matricula")).intValue();

                // pega info da matrícula + curso
                Document m = matriculas.find(Filters.eq("id_matricula", idMat)).first();
                String nomeCurso = "(desconhecido)";
                if (m != null) {
                    int idCurso = ((Number) m.get("id_curso")).intValue();
                    Document c = cursos.find(Filters.eq("id_curso", idCurso)).first();
                    if (c != null) {
                        nomeCurso = c.getString("nome_curso");
                    }
                }

                // aqui entra o nome do estudante ao lado da matrícula
                lista.add(String.format("#%d | matrícula %d (%s) | %s | %s | nota: %.2f",
                        idNota, idMat, nomeEstudante, nomeCurso, semestre, nota));
            }

        } catch (Exception e) {
            System.err.println("Erro ao listar notas do estudante no Mongo: " + e.getMessage());
            return;
        }

        if (lista.isEmpty()) {
            System.out.println("(Nenhuma nota encontrada para esse estudante)");
        } else {
            lista.forEach(System.out::println);
        }

    }

    // INSERIR NOTA ESCOLHENDO PELO ESTUDANTE
    public void inserirPorEstudanteId(Scanner in) {
        System.out.print("ID estudante: ");
        int idEst = Integer.parseInt(in.nextLine());

        // 1) buscar estudante
        Document est = estudantes.find(Filters.eq("id_estudante", idEst)).first();
        if (est == null) {
            System.out.println("Estudante não encontrado.");
            return;
        }

        String nomeEst = est.getString("nome");
        System.out.println("Estudante: " + nomeEst);

        // 2) listar matrículas desse estudante
        List<Integer> idsMat = new ArrayList<>();

        try (MongoCursor<Document> cursor = matriculas.find(Filters.eq("id_estudante", idEst))
                .sort(Sorts.ascending("id_matricula"))
                .iterator()) {

            System.out.println("\nMATRÍCULAS DO ESTUDANTE:");
            while (cursor.hasNext()) {
                Document m = cursor.next();
                int idMat = ((Number) m.get("id_matricula")).intValue();
                idsMat.add(idMat);

                int idCurso = ((Number) m.get("id_curso")).intValue();
                String status = m.getString("status_matricula");
                String dataStr = m.getString("data_matricula");
                LocalDate dataMat = null;
                if (dataStr != null && !dataStr.isBlank()) {
                    dataMat = LocalDate.parse(dataStr);
                }

                Document c = cursos.find(Filters.eq("id_curso", idCurso)).first();
                String nomeCurso = c != null ? c.getString("nome_curso") : "(desconhecido)";

                System.out.printf("  %d - %s | %s | %s%n",
                        idMat,
                        nomeCurso,
                        status,
                        dataMat != null ? dataMat : "");
            }
        }

        if (idsMat.isEmpty()) {
            System.out.println("Estudante não possui matrículas para receber nota.");
            return;
        }

        System.out.print("\nEscolha o ID da matrícula para lançar a nota: ");
        int idMatEscolhido = Integer.parseInt(in.nextLine());
        if (!idsMat.contains(idMatEscolhido)) {
            System.out.println("Matrícula inválida para este estudante.");
            return;
        }

        double nota = lerDouble(in, "Nota do estudante: ");
        System.out.print("Semestre (ex: 2024/1): ");
        String semestre = in.nextLine();

        try {
            int idNota = getNextId(notas, "id_nota");

            Document doc = new Document()
                    .append("id_nota", idNota)
                    .append("id_matricula", idMatEscolhido)
                    .append("nota_estudante", nota)
                    .append("semestre", semestre);

            notas.insertOne(doc);
            System.out.println("Nota inserida! (id_nota = " + idNota + ")");
        } catch (Exception e) {
            System.err.println("Erro ao inserir nota no Mongo: " + e.getMessage());
        }
    }
}
