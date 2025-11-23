// seleciona o nome do banco (equivalente ao USE labdatabase no create_tables_estudantes.sql)
use("");

// DROPAR coleções se existirem
db.notas.drop();
db.matriculas.drop();
db.cursos.drop();
db.estudantes.drop();

// As coleções são criadas automaticamente quando você insere
// ou cria índices. Aqui a gente só garante índices equivalentes às PK/UNIQUE:

// === ESTUDANTES ===
db.estudantes.createIndex({ id_estudante: 1 }, { unique: true });
db.estudantes.createIndex({ cpf: 1 }, { unique: true });
db.estudantes.createIndex({ email: 1 }, { unique: true });

// === CURSOS ===
db.cursos.createIndex({ id_curso: 1 }, { unique: true });

// === MATRICULAS ===
db.matriculas.createIndex({ id_matricula: 1 }, { unique: true });
db.matriculas.createIndex({ id_estudante: 1 });
db.matriculas.createIndex({ id_curso: 1 });
// UNIQUE (id_estudante, id_curso) do MySQL:
db.matriculas.createIndex(
  { id_estudante: 1, id_curso: 1 },
  { unique: true }
);

// === NOTAS ===
db.notas.createIndex({ id_nota: 1 }, { unique: true });
db.notas.createIndex({ id_matricula: 1 });
