package br.com.fiap.cuidar.dao;

import br.com.fiap.cuidar.exception.ConflitoException;
import br.com.fiap.cuidar.exception.PersistenciaException;
import br.com.fiap.cuidar.models.PessoaAcompanhada;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PessoaAcompanhadaDAO {
    private static final String COLUNAS = "CD_PESSOA_ACOMPANHADA, CD_USUARIO, NM_PESSOA, DT_NASCIMENTO, DS_OBSERVACAO, NR_TAMANHO_FONTE, FL_ALTO_CONTRASTE";
    private final DataSource dataSource;

    public PessoaAcompanhadaDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public PessoaAcompanhada cadastrar(PessoaAcompanhada pessoa) {
        try (Connection connection = dataSource.getConnection()) {
            return inserir(connection, pessoa);
        } catch (SQLException e) { throw traduzir(e); }
    }

    public PessoaAcompanhada cadastrarComFamiliar(PessoaAcompanhada pessoa, long familiarId) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                inserir(connection, pessoa);
                try (PreparedStatement vinculo = connection.prepareStatement("""
                        INSERT INTO TB_USUARIO_PESSOA_CUIDADO (CD_USUARIO, CD_PESSOA_ACOMPANHADA)
                        VALUES (?, ?)
                        """)) {
                    vinculo.setLong(1, familiarId);
                    vinculo.setLong(2, pessoa.getId());
                    vinculo.executeUpdate();
                }
                connection.commit();
                return pessoa;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
                throw e;
            }
        } catch (SQLException e) { throw traduzir(e); }
    }

    private PessoaAcompanhada inserir(Connection connection, PessoaAcompanhada pessoa) throws SQLException {
        String sql = "INSERT INTO TB_PESSOA_ACOMPANHADA (" + COLUNAS
                + ") VALUES (SQ_TB_PESSOA_ACOMPANHADA.NEXTVAL, ?, ?, ?, ?, ?, ?)";
        // Informar a coluna evita que o driver Oracle retorne apenas o ROWID.
        try (PreparedStatement statement = connection.prepareStatement(sql, new String[]{"CD_PESSOA_ACOMPANHADA"})) {
            preencher(statement, pessoa);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) { throw new PersistenciaException(new IllegalStateException("ID não retornado.")); }
                pessoa.setId(keys.getLong(1));
            }
            return pessoa;
        }
    }

    public List<PessoaAcompanhada> listar() {
        String sql = "SELECT " + COLUNAS + " FROM TB_PESSOA_ACOMPANHADA ORDER BY CD_PESSOA_ACOMPANHADA";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<PessoaAcompanhada> pessoas = new ArrayList<>();
            while (result.next()) { pessoas.add(ler(result)); }
            return pessoas;
        } catch (SQLException e) { throw traduzir(e); }
    }

    public Optional<PessoaAcompanhada> buscarPorId(Long id) {
        String sql = "SELECT " + COLUNAS + " FROM TB_PESSOA_ACOMPANHADA WHERE CD_PESSOA_ACOMPANHADA = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(ler(result)) : Optional.empty();
            }
        } catch (SQLException e) { throw traduzir(e); }
    }

    public boolean atualizar(PessoaAcompanhada pessoa) {
        String sql = """
                UPDATE TB_PESSOA_ACOMPANHADA SET CD_USUARIO = ?, NM_PESSOA = ?, DT_NASCIMENTO = ?,
                DS_OBSERVACAO = ?, NR_TAMANHO_FONTE = ?, FL_ALTO_CONTRASTE = ?
                WHERE CD_PESSOA_ACOMPANHADA = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            preencher(statement, pessoa);
            statement.setLong(7, pessoa.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) { throw traduzir(e); }
    }

    public boolean excluir(Long id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM TB_PESSOA_ACOMPANHADA WHERE CD_PESSOA_ACOMPANHADA = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) { throw traduzir(e); }
    }

    private void preencher(PreparedStatement statement, PessoaAcompanhada pessoa) throws SQLException {
        if (pessoa.getUsuarioId() == null) { statement.setNull(1, Types.NUMERIC); }
        else { statement.setLong(1, pessoa.getUsuarioId()); }
        statement.setString(2, pessoa.getNome());
        statement.setDate(3, Date.valueOf(pessoa.getDataNascimento()));
        if (pessoa.getObservacao() == null) { statement.setNull(4, Types.VARCHAR); }
        else { statement.setString(4, pessoa.getObservacao()); }
        statement.setInt(5, pessoa.getTamanhoFonte());
        statement.setString(6, pessoa.getAltoContraste());
    }

    private PessoaAcompanhada ler(ResultSet result) throws SQLException {
        PessoaAcompanhada pessoa = new PessoaAcompanhada();
        pessoa.setId(result.getLong("CD_PESSOA_ACOMPANHADA"));
        long usuarioId = result.getLong("CD_USUARIO");
        pessoa.setUsuarioId(result.wasNull() ? null : usuarioId);
        pessoa.setNome(result.getString("NM_PESSOA"));
        pessoa.setDataNascimento(result.getDate("DT_NASCIMENTO").toLocalDate());
        pessoa.setObservacao(result.getString("DS_OBSERVACAO"));
        pessoa.setTamanhoFonte(result.getInt("NR_TAMANHO_FONTE"));
        pessoa.setAltoContraste(result.getString("FL_ALTO_CONTRASTE"));
        return pessoa;
    }

    private RuntimeException traduzir(SQLException e) {
        for (SQLException atual = e; atual != null; atual = atual.getNextException()) {
            switch (atual.getErrorCode()) {
                case 1:
                    return new ConflitoException("O usuário já está vinculado a outra pessoa acompanhada.", e);
                case 2291:
                    return new ConflitoException("O usuário informado não existe.", e);
                case 2292:
                    return new ConflitoException("A pessoa acompanhada possui registros relacionados e não pode ser removida.", e);
                default:
                    break;
            }
        }
        return new PersistenciaException(e);
    }
}
