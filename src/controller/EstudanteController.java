package controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.bson.Document;

import conexion.*;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

public class EstudanteController {

    // ---------------- COLEÇÕES DO MONGO ----------------------------------
    private final MongoCollection<Document> estudantes;
    private final MongoCollection<Document> cursos;
    private final MongoCollection<Document> matriculas;
    private final MongoCollection<Document> notas;

    public EstudanteController() {
        MongoDatabase db = MongoConnection.getDatabase();
        this.estudantes = db.getCollection("estudantes");
        this.cursos = db.getCollection("cursos");
        this.matriculas = db.getCollection("matriculas");
        this.notas = db.getCollection("notas");
    }

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

    // ---------------- MÉTODOS ----------------------------------

    // INSERIR (estudante + matrícula)
    public void inserir(Scanner in) {
        System.out.print("Nome: ");
        String nome = in.nextLine();

        System.out.print("Data nascimento (AAAA-MM-DD): ");
        LocalDate dn = LocalDate.parse(in.nextLine());

        System.out.print("CPF (apenas números): ");
        String cpf = in.nextLine();

        System.out.print("Email: ");
        String email = in.nextLine();

        try {
            // 1) gera ID do estudante
            int idEstudante = getNextId(estudantes, "id_estudante");

            // 2) cria documento do estudante
            Document estudanteDoc = new Document()
                    .append("id_estudante", idEstudante)
                    .append("nome", nome)
                    .append("data_nascimento", dn.toString()) // armazenado como String yyyy-MM-dd
                    .append("cpf", cpf)
                    .append("email", email);

            estudantes.insertOne(estudanteDoc);

            // 3) lista cursos para escolher (igual fazia no MySQL)
            System.out.println("\nCURSOS disponíveis:");
            try (MongoCursor<Document> cursor = cursos.find()
                    .sort(Sorts.ascending("id_curso"))
                    .iterator()) {

                while (cursor.hasNext()) {
                    Document c = cursor.next();
                    System.out.printf("  %d - %s%n",
                            c.getInteger("id_curso"),
                            c.getString("nome_curso"));
                }
            }

            System.out.print("ID do curso para matrícula: ");
            int idCurso = Integer.parseInt(in.nextLine());

            // (opcional) validar se o curso existe
            Document cursoDoc = cursos.find(Filters.eq("id_curso", idCurso)).first();
            if (cursoDoc == null) {
                System.err.println("Curso não encontrado. Matrícula não criada.");
                return;
            }

            // 4) cria matricula ATIVA para hoje
            int idMatricula = getNextId(matriculas, "id_matricula");

            Document matriculaDoc = new Document()
                    .append("id_matricula", idMatricula)
                    .append("id_estudante", idEstudante)
                    .append("id_curso", idCurso)
                    .append("data_matricula", LocalDate.now().toString())
                    .append("status_matricula", "Ativo"); // igual ao CHECK do SQL

            matriculas.insertOne(matriculaDoc);

            System.out.println("Estudante e matrícula criados com sucesso!");

        } catch (MongoWriteException dup) {
            // cai aqui em caso de duplicidade
            System.err.println("CPF/email duplicado ou violação de índice único.");
        } catch (Exception e) {
            System.err.println("Erro ao inserir estudante/matrícula no Mongo: " + e.getMessage());
        }
    }

    //ATUALIZAR
    public void atualizar(Scanner in) {
        listar(); // mostra para escolher

        System.out.print("ID do estudante: ");
        int id = Integer.parseInt(in.nextLine());

        System.out.print("Nome: ");
        String nome = in.nextLine();

        System.out.print("Data nascimento (AAAA-MM-DD): ");
        LocalDate dn = LocalDate.parse(in.nextLine());

        System.out.print("CPF: ");
        String cpf = in.nextLine();

        System.out.print("Email: ");
        String email = in.nextLine();

        try {
            Document filtro = new Document("id_estudante", id);

            Document novosDados = new Document()
                    .append("nome", nome)
                    .append("data_nascimento", dn.toString())
                    .append("cpf", cpf)
                    .append("email", email);

            Document update = new Document("$set", novosDados);

            long modificados = estudantes.updateOne(filtro, update).getModifiedCount();
            System.out.println(modificados > 0 ? "Atualizado!" : "Nada atualizado.");

        } catch (MongoWriteException dup) {
            System.err.println("CPF ou e-mail já existe.");
        } catch (Exception e) {
            System.err.println("Erro ao atualizar estudante no Mongo: " + e.getMessage());
        }
    }

    //REMOVER
     public void remover(Scanner in) {
        listar();
        System.out.print("ID do estudante: ");
        int id = Integer.parseInt(in.nextLine());

        try {
            // 0) verifica se existe
            Document est = estudantes.find(Filters.eq("id_estudante", id)).first();
            if (est == null) {
                System.out.println("ID não encontrado. Nada a remover.");
                return;
            }

            // 1) pega todas as matriculas desse estudante
            List<Integer> idsMatriculas = new ArrayList<>();
            try (MongoCursor<Document> cursor = matriculas.find(Filters.eq("id_estudante", id)).iterator()) {
                while (cursor.hasNext()) {
                    Document m = cursor.next();
                    idsMatriculas.add(((Number) m.get("id_matricula")).intValue());
                }
            }

            // 2) apaga as NOTAS dessas matriculas
            if (!idsMatriculas.isEmpty()) {
                notas.deleteMany(Filters.in("id_matricula", idsMatriculas));
            }

            // 3) apaga as MATRICULAS do estudante
            matriculas.deleteMany(Filters.eq("id_estudante", id));

            // 4) apaga o ESTUDANTE
            long removidos = estudantes.deleteOne(Filters.eq("id_estudante", id)).getDeletedCount();

            System.out.println(removidos > 0 ? "Removido!" : "Nada removido.");
        } catch (Exception e) {
            System.err.println("Erro ao remover estudante no Mongo: " + e.getMessage());
        }
    }

    //LISTAR
public void listar() {
    try (MongoCursor<Document> cursor = estudantes.find()
            .sort(Sorts.ascending("id_estudante"))
            .iterator()) {

        // Cabeçalho
        System.out.printf("%-4s | %-25s | %-25s | %-15s | %-12s%n",
                "ID", "Nome do estudante", "E-mail", "CPF", "Data de Nasc.");
        System.out.println("-----+---------------------------+---------------------------+-----------------+--------------");

        boolean vazio = true;

        while (cursor.hasNext()) {
            vazio = false;
            Document doc = cursor.next();

            int id = ((Number) doc.get("id_estudante")).intValue();
            String nome = doc.getString("nome");
            String email = doc.getString("email");
            String cpf = maskCpf(doc.getString("cpf"));

            String dataStr = doc.getString("data_nascimento");
            String dataNasc = "";
            if (dataStr != null && !dataStr.isBlank()) {
                dataNasc = dataStr; // já está no formato yyyy-MM-dd
            }

            // Linha formatada
            System.out.printf("%-4s | %-25s | %-25s | %-15s | %-12s%n",
                    "#" + id, nome, email, cpf, dataNasc);
        }

        if (vazio) {
            System.out.println("(Nenhum estudante cadastrado)");
        }

    } catch (Exception e) {
        System.err.println("Erro ao listar estudantes no Mongo: " + e.getMessage());
    }
}


    // mascara CPF
    private static String maskCpf(String cpf) {
        if (cpf == null)
            return "";
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11)
            return cpf;
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }
}
