package controller;

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

public class CursoController {
    // Coleção
    private final MongoCollection<Document> cursos;
    private final MongoCollection<Document> matriculas;
    private final MongoCollection<Document> notas;

    public CursoController() {
        MongoDatabase db = MongoConnection.getDatabase();
        this.cursos = db.getCollection("cursos");
        this.matriculas = db.getCollection("matriculas");
        this.notas = db.getCollection("notas");
    }

    // Gera próximo ID inteiro baseado no maior já existente
    private int getNextId(MongoCollection<Document> col, String field) {
        Document last = col.find()
                .sort(Sorts.descending(field))
                .first();

        if (last == null || last.get(field) == null) {
            return 1;
        }

        Object value = last.get(field);
        if (value instanceof Number) {
            return ((Number) value).intValue() + 1;
        }
        return 1;
    }


    // ================== MÉTODOS ==================

    //INSERIR CURSO
    public void inserir(Scanner in) {
        System.out.print("Nome do curso: ");
        String nome = in.nextLine();

        System.out.print("Carga horária - CH: ");
        int ch = Integer.parseInt(in.nextLine());

        try {
            int idCurso = getNextId(cursos, "id_curso");

            Document doc = new Document()
                    .append("id_curso", idCurso)
                    .append("nome_curso", nome)
                    .append("carga_horaria", ch);

            cursos.insertOne(doc);
            System.out.println("Curso inserido! (id_curso = " + idCurso + ")");
        } catch (Exception e) {
            System.err.println("Erro ao inserir curso no Mongo: " + e.getMessage());
        }
    }

    //ATUALIZAR CURSO 
    public void atualizar(Scanner in) {
        listar();

        System.out.print("ID do curso: ");
        int id = Integer.parseInt(in.nextLine());

        System.out.print("Novo nome: ");
        String nome = in.nextLine();

        System.out.print("Nova carga horária: ");
        int ch = Integer.parseInt(in.nextLine());

        try {
            Document filtro = new Document("id_curso", id);

            Document novosDados = new Document()
                    .append("nome_curso", nome)
                    .append("carga_horaria", ch);

            Document update = new Document("$set", novosDados);

            long modificados = cursos.updateOne(filtro, update).getModifiedCount();
            System.out.println(modificados > 0 ? "Curso atualizado!" : "Nada atualizado.");
        } catch (Exception e) {
            System.err.println("Erro ao atualizar curso no Mongo: " + e.getMessage());
        }
    }

    //REMOVER CURSO
    public void remover(Scanner in) {
        listar();

        System.out.print("ID do curso: ");
        int id = Integer.parseInt(in.nextLine());

        try {
            // verifica se curso existe
            Document cursoDoc = cursos.find(Filters.eq("id_curso", id)).first();
            if (cursoDoc == null) {
                System.out.println("Curso não encontrado.");
                return;
            }

            // 1) pega matrículas do curso
            List<Integer> idsMatriculas = new ArrayList<>();
            try (MongoCursor<Document> cursor = matriculas
                    .find(Filters.eq("id_curso", id))
                    .iterator()) {
                while (cursor.hasNext()) {
                    Document m = cursor.next();
                    idsMatriculas.add(((Number) m.get("id_matricula")).intValue());
                }
            }

            long qtdMats = idsMatriculas.size();
            long qtdNotas = 0;

            // 2) conta notas ligadas a essas matrículas
            if (!idsMatriculas.isEmpty()) {
                qtdNotas = notas.countDocuments(Filters.in("id_matricula", idsMatriculas));
            }

            System.out.printf(
                    "Isso vai excluir %d nota(s) e %d matrícula(s) desse curso.%n",
                    qtdNotas, qtdMats);
            System.out.print("Confirmar exclusão? (S/N): ");
            String resp = in.nextLine();
            if (!resp.equalsIgnoreCase("s")) {
                System.out.println("Operação cancelada.");
                return;
            }

            // 3) apaga notas
            if (!idsMatriculas.isEmpty()) {
                notas.deleteMany(Filters.in("id_matricula", idsMatriculas));
            }

            // 4) apaga matrículas
            matriculas.deleteMany(Filters.eq("id_curso", id));

            // 5) apaga o curso
            long removidos = cursos.deleteOne(Filters.eq("id_curso", id)).getDeletedCount();
            System.out.println(removidos > 0 ? "Curso removido!" : "Nada removido.");

        } catch (Exception e) {
            System.err.println("Erro ao remover curso no Mongo: " + e.getMessage());
        }
    }

     //LISTAR CURSOS
   public void listar() {
    try (MongoCursor<Document> cursor = cursos.find()
            .sort(Sorts.ascending("id_curso"))
            .iterator()) {

        System.out.println("\n-- CURSOS --");
        System.out.printf("%-4s | %-35s | %-5s%n",
                "ID", "Nome do curso", "CH");
        System.out.println("-----+-------------------------------------+------");

        boolean vazio = true;

        while (cursor.hasNext()) {
            vazio = false;
            Document doc = cursor.next();

            int id = ((Number) doc.get("id_curso")).intValue();
            String nome = doc.getString("nome_curso");
            int ch = doc.getInteger("carga_horaria", 0);

            System.out.printf("%-4s | %-35s | %-5d%n",
                    "#" + id, nome, ch);
        }

        if (vazio) {
            System.out.println("(Nenhum curso cadastrado)");
        }

    } catch (Exception e) {
        System.err.println("Erro ao listar cursos no Mongo: " + e.getMessage());
    }
}
}
