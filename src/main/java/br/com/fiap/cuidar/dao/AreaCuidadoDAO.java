package br.com.fiap.cuidar.dao;

import br.com.fiap.cuidar.exception.ConflitoException;
import br.com.fiap.cuidar.exception.PersistenciaException;
import br.com.fiap.cuidar.models.AreaCuidado;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AreaCuidadoDAO {
    private static final String COLUNAS = "CD_AREA, NM_AREA, DS_AREA";
    private final DataSource dataSource;

    public AreaCuidadoDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public AreaCuidado cadastrar(AreaCuidado area) {
        String sql = "INSERT INTO TB_AREA_CUIDADO (" + COLUNAS
                + ") VALUES (SQ_TB_AREA_CUIDADO.NEXTVAL, ?, ?)";
        // A coluna explícita faz o driver Oracle devolver o ID em vez do ROWID.
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, new String[]{"CD_AREA"})) {
            preencher(statement, area);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) { throw new PersistenciaException(new IllegalStateException("ID não retornado.")); }
                area.setId(keys.getLong(1));
            }
            return area;
        } catch (SQLException e) { throw traduzir(e); }
    }

    public List<AreaCuidado> listar() {
        String sql = "SELECT " + COLUNAS + " FROM TB_AREA_CUIDADO ORDER BY CD_AREA";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return consultarAreas(statement);
        } catch (SQLException e) { throw traduzir(e); }
    }

    public List<AreaCuidado> listarPorPessoa(Long pessoaId) {
        String sql = """
                SELECT a.CD_AREA, a.NM_AREA, a.DS_AREA
                FROM TB_PESSOA_AREA_CUIDADO pa
                JOIN TB_AREA_CUIDADO a ON a.CD_AREA = pa.CD_AREA
                WHERE pa.CD_PESSOA_ACOMPANHADA = ?
                ORDER BY a.CD_AREA
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pessoaId);
            return consultarAreas(statement);
        } catch (SQLException e) { throw traduzir(e); }
    }

    public Optional<AreaCuidado> buscarPorId(Long id) {
        String sql = "SELECT " + COLUNAS + " FROM TB_AREA_CUIDADO WHERE CD_AREA = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(ler(result)) : Optional.empty();
            }
        } catch (SQLException e) { throw traduzir(e); }
    }

    public void habilitarParaPessoa(Long pessoaId, Long areaId) {
        String sql = """
                INSERT INTO TB_PESSOA_AREA_CUIDADO (CD_PESSOA_AREA, CD_PESSOA_ACOMPANHADA, CD_AREA, DT_INCLUSAO)
                VALUES (SQ_TB_PESSOA_AREA_CUIDADO.NEXTVAL, ?, ?, SYSDATE)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pessoaId);
            statement.setLong(2, areaId);
            statement.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1) {
                throw new ConflitoException("A área já está habilitada para a pessoa acompanhada.", e);
            }
            if (e.getErrorCode() == 2291) {
                throw new ConflitoException("A pessoa acompanhada ou a área deixou de estar disponível.", e);
            }
            throw new PersistenciaException(e);
        }
    }

    public boolean atualizar(AreaCuidado area) {
        String sql = "UPDATE TB_AREA_CUIDADO SET NM_AREA = ?, DS_AREA = ? WHERE CD_AREA = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            preencher(statement, area);
            statement.setLong(3, area.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) { throw traduzir(e); }
    }

    public boolean excluir(Long id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM TB_AREA_CUIDADO WHERE CD_AREA = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) { throw traduzir(e); }
    }

    private void preencher(PreparedStatement statement, AreaCuidado area) throws SQLException {
        statement.setString(1, area.getNome());
        if (area.getDescricao() == null) { statement.setNull(2, Types.VARCHAR); }
        else { statement.setString(2, area.getDescricao()); }
    }

    private List<AreaCuidado> consultarAreas(PreparedStatement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery()) {
            List<AreaCuidado> areas = new ArrayList<>();
            while (result.next()) { areas.add(ler(result)); }
            return areas;
        }
    }

    private AreaCuidado ler(ResultSet result) throws SQLException {
        AreaCuidado area = new AreaCuidado();
        area.setId(result.getLong("CD_AREA"));
        area.setNome(result.getString("NM_AREA"));
        area.setDescricao(result.getString("DS_AREA"));
        return area;
    }

    private RuntimeException traduzir(SQLException e) {
        for (SQLException atual = e; atual != null; atual = atual.getNextException()) {
            switch (atual.getErrorCode()) {
                case 1:
                    return new ConflitoException("Já existe uma área de cuidado com esse nome.", e);
                case 2292:
                    return new ConflitoException("A área de cuidado possui registros relacionados e não pode ser removida.", e);
                default:
                    break;
            }
        }
        return new PersistenciaException(e);
    }
}
