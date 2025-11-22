package reports;

import conexion.MongoConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

public class Relatorios {
    private static final double MEDIA_APROVACAO_PADRAO = 7.0;

    // ======= COLEÇÕES MONGO =======
    private final MongoCollection<Document> notas;
    private final MongoCollection<Document> matriculas;
    private final MongoCollection<Document> cursos;

    public Relatorios() {
        MongoDatabase db = MongoConnection.getDatabase();
        this.notas = db.getCollection("notas");
        this.matriculas = db.getCollection("matriculas");
        this.cursos = db.getCollection("cursos");
    }

    /**
     * Relatório 1: Média por curso e semestre.
     * Agrupa por curso e semestre, considerando as notas lançadas.
     */
    public void mediaPorCursoESemestre() {
        // Carregar todas as matrículas em memória: id_matricula -> doc
        Map<Integer, Document> matriculasPorId = new HashMap<>();
        try (MongoCursor<Document> cur = matriculas.find().iterator()) {
            while (cur.hasNext()) {
                Document m = cur.next();
                Object idMatObj = m.get("id_matricula");
                if (idMatObj instanceof Number) {
                    int idMat = ((Number) idMatObj).intValue();
                    matriculasPorId.put(idMat, m);
                }
            }
        }

        // Carregar todos os cursos em memória: id_curso -> doc
        Map<Integer, Document> cursosPorId = new HashMap<>();
        try (MongoCursor<Document> cur = cursos.find().iterator()) {
            while (cur.hasNext()) {
                Document c = cur.next();
                Object idCursoObj = c.get("id_curso");
                if (idCursoObj instanceof Number) {
                    int idCurso = ((Number) idCursoObj).intValue();
                    cursosPorId.put(idCurso, c);
                }
            }
        }

        // Estrutura para acumular soma e contagem por (curso, semestre)
        class StatsMedia {
            int idCurso;
            String nomeCurso;
            String semestre;
            double somaNotas;
            int qtdNotas;
        }

        Map<String, StatsMedia> mapa = new HashMap<>();

        // Percorrer TODAS as notas e agrupar por curso+semestre
        try (MongoCursor<Document> curNotas = notas.find().iterator()) {
            while (curNotas.hasNext()) {
                Document n = curNotas.next();

                Object notaObj = n.get("nota_estudante");
                Object semestreObj = n.get("semestre");
                Object idMatObj = n.get("id_matricula");

                if (!(notaObj instanceof Number) || !(semestreObj instanceof String) || !(idMatObj instanceof Number)) {
                    continue; // ignora registros com dados faltando/estranhos
                }

                double nota = ((Number) notaObj).doubleValue();
                String semestre = (String) semestreObj;
                int idMat = ((Number) idMatObj).intValue();

                Document m = matriculasPorId.get(idMat);
                if (m == null) continue; // matrícula não encontrada

                Object idCursoObj = m.get("id_curso");
                if (!(idCursoObj instanceof Number)) continue;

                int idCurso = ((Number) idCursoObj).intValue();
                Document c = cursosPorId.get(idCurso);
                if (c == null) continue; // curso não encontrado

                String nomeCurso = c.getString("nome_curso");
                if (nomeCurso == null) nomeCurso = "(sem nome)";

                String chave = idCurso + "|" + nomeCurso + "|" + semestre;
                StatsMedia stats = mapa.get(chave);
                if (stats == null) {
                    stats = new StatsMedia();
                    stats.idCurso = idCurso;
                    stats.nomeCurso = nomeCurso;
                    stats.semestre = semestre;
                    stats.somaNotas = 0.0;
                    stats.qtdNotas = 0;
                    mapa.put(chave, stats);
                }

                stats.somaNotas += nota;
                stats.qtdNotas++;
            }
        }

        // Transformar em lista e ordenar por nomeCurso, semestre
        List<StatsMedia> lista = new ArrayList<>(mapa.values());
        lista.sort((a, b) -> {
            int cmp = a.nomeCurso.compareToIgnoreCase(b.nomeCurso);
            if (cmp != 0) return cmp;
            return a.semestre.compareToIgnoreCase(b.semestre);
        });

        // Impressão
        System.out.println("\n== MÉDIA POR CURSO E SEMESTRE ==");
        System.out.printf("%-4s  %-30s  %-10s  %-6s%n",
                "ID", "CURSO", "SEMESTRE", "MÉDIA");
        System.out.println("---------------------------------------------------------------");

        for (StatsMedia s : lista) {
            double media = s.qtdNotas > 0 ? (s.somaNotas / s.qtdNotas) : 0.0;
            System.out.printf("%-4d  %-30s  %-10s  %-6.2f%n",
                    s.idCurso, s.nomeCurso, s.semestre, media);
        }

        if (lista.isEmpty()) {
            System.out.println("(Nenhum dado encontrado para médias por curso e semestre)");
        }
    }

    /**
     * Relatório 2: Desempenho por curso com média de aprovação definida no código.
     * Chama a versão parametrizada com o corte padrão.
     */
    public void desempenhoPorCurso() {
        desempenhoPorCurso(MEDIA_APROVACAO_PADRAO);
    }

    /**
     * Relatório 2 (parametrizado): Desempenho por curso.
     * Calcula: Média geral, quantidade de notas, aprovados, reprovados e taxa de aprovação.
     */
    public void desempenhoPorCurso(double mediaAprovacao) {
        // Carregar matrículas em memória
        Map<Integer, Document> matriculasPorId = new HashMap<>();
        try (MongoCursor<Document> cur = matriculas.find().iterator()) {
            while (cur.hasNext()) {
                Document m = cur.next();
                Object idMatObj = m.get("id_matricula");
                if (idMatObj instanceof Number) {
                    int idMat = ((Number) idMatObj).intValue();
                    matriculasPorId.put(idMat, m);
                }
            }
        }

        // Carregar cursos em memória
        Map<Integer, Document> cursosPorId = new HashMap<>();
        try (MongoCursor<Document> cur = cursos.find().iterator()) {
            while (cur.hasNext()) {
                Document c = cur.next();
                Object idCursoObj = c.get("id_curso");
                if (idCursoObj instanceof Number) {
                    int idCurso = ((Number) idCursoObj).intValue();
                    cursosPorId.put(idCurso, c);
                }
            }
        }

        // Estrutura para acumular dados por curso
        class StatsCurso {
            int idCurso;
            String nomeCurso;
            double somaNotas;
            int qtdNotas;
            int aprovados;
            int reprovados;
        }

        Map<Integer, StatsCurso> mapa = new HashMap<>();

        // Percorrer todas as notas e acumular por curso
        try (MongoCursor<Document> curNotas = notas.find().iterator()) {
            while (curNotas.hasNext()) {
                Document n = curNotas.next();

                Object notaObj = n.get("nota_estudante");
                Object idMatObj = n.get("id_matricula");
                if (!(notaObj instanceof Number) || !(idMatObj instanceof Number)) {
                    continue;
                }

                double nota = ((Number) notaObj).doubleValue();
                int idMat = ((Number) idMatObj).intValue();

                Document m = matriculasPorId.get(idMat);
                if (m == null) continue;

                Object idCursoObj = m.get("id_curso");
                if (!(idCursoObj instanceof Number)) continue;

                int idCurso = ((Number) idCursoObj).intValue();
                Document c = cursosPorId.get(idCurso);
                if (c == null) continue;

                String nomeCurso = c.getString("nome_curso");
                if (nomeCurso == null) nomeCurso = "(sem nome)";

                StatsCurso stats = mapa.get(idCurso);
                if (stats == null) {
                    stats = new StatsCurso();
                    stats.idCurso = idCurso;
                    stats.nomeCurso = nomeCurso;
                    stats.somaNotas = 0.0;
                    stats.qtdNotas = 0;
                    stats.aprovados = 0;
                    stats.reprovados = 0;
                    mapa.put(idCurso, stats);
                }

                stats.somaNotas += nota;
                stats.qtdNotas++;

                if (nota >= mediaAprovacao) {
                    stats.aprovados++;
                } else {
                    stats.reprovados++;
                }
            }
        }

        // Transformar em lista e ordenar por nome do curso (igual ORDER BY c.nome_curso)
        List<StatsCurso> lista = new ArrayList<>(mapa.values());
        lista.sort((a, b) -> a.nomeCurso.compareToIgnoreCase(b.nomeCurso));

        // Impressão
        System.out.println("\n== DESEMPENHO POR CURSO (corte: " + mediaAprovacao + ") ==");
        System.out.printf("%-4s  %-30s  %-6s  %-8s  %-9s  %-10s  %-6s%n",
                "ID", "CURSO", "MÉDIA", "QTD_NOTA", "APROVADOS", "REPROVADOS", "%APR");
        System.out.println("-------------------------------------------------------------------------------");

        for (StatsCurso s : lista) {
            double mediaGeral = s.qtdNotas > 0 ? (s.somaNotas / s.qtdNotas) : 0.0;
            double taxa = s.qtdNotas > 0 ? ((double) s.aprovados / s.qtdNotas) * 100.0 : 0.0;

            System.out.printf("%-4d  %-30s  %-6.2f  %-8d  %-9d  %-10d  %-6.2f%n",
                    s.idCurso, s.nomeCurso, mediaGeral, s.qtdNotas,
                    s.aprovados, s.reprovados, taxa);
        }

        if (lista.isEmpty()) {
            System.out.println("(Nenhum dado encontrado para desempenho por curso)");
        }
    }

}
