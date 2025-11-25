package utils;

import com.mongodb.client.MongoDatabase;
import conexion.MongoConnection;

public class splash_screen {

    
    // Exibe a contagem de registros inseridos em cada tabela
     
    public static void exibirContagemRegistros() {
        MongoDatabase db = MongoConnection.getDatabase();

        System.out.println("\n= CONTAGEM DE REGISTROS EXISTENTES  =");
        System.out.println("=====================================");

        // Contagem de cada coleção
        long totalEstudantes = db.getCollection("estudantes").countDocuments();
        long totalCursos     = db.getCollection("cursos").countDocuments();
        long totalMatriculas = db.getCollection("matriculas").countDocuments();
        long totalNotas      = db.getCollection("notas").countDocuments();

        System.out.println("Total de Estudantes : " + totalEstudantes);
        System.out.println("Total de Cursos     : " + totalCursos);
        System.out.println("Total de Matrículas : " + totalMatriculas);
        System.out.println("Total de Notas      : " + totalNotas);

        long totalGeral = totalEstudantes + totalCursos + totalMatriculas + totalNotas;

        System.out.println("=====================================");
        System.out.println("TOTAL GERAL DE REGISTROS: " + totalGeral);
        System.out.println("=====================================\n");
    }
}