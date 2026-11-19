package br.com.fiap.cuidar.dao;

import br.com.fiap.cuidar.exception.PersistenciaException;
import br.com.fiap.cuidar.models.Conta;
import br.com.fiap.cuidar.models.Conta.Perfil;
import br.com.fiap.cuidar.models.VinculoProfissional;
import br.com.fiap.cuidar.models.VinculoProfissional.Permissao;
import br.com.fiap.cuidar.exception.PessoaAcompanhadaNaoEncontradaException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class VinculoCuidadoDAO {
    private static final String PROFISSIONAIS = """
            SELECT v.CD_VINCULO, v.CD_PESSOA_ACOMPANHADA, v.CD_USUARIO, u.NM_USUARIO,
                   v.TP_PERMISSAO, v.ST_AUTORIZACAO
            FROM TB_VINCULO v JOIN TB_USUARIO u ON u.CD_USUARIO = v.CD_USUARIO
            WHERE v.TP_RELACAO = 'PROFISSIONAL' AND u.TP_USUARIO = 'PROFISSIONAL'
            """;
    private static final String VINCULOS_ATIVOS = """
            SELECT CD_PESSOA_ACOMPANHADA FROM TB_VINCULO
            WHERE CD_USUARIO = ? AND ST_AUTORIZACAO = 'ATIVO'
            """;
    private final JdbcTemplate jdbc;

    public VinculoCuidadoDAO(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
    }

    public Set<Long> pessoasPermitidas(Conta conta) {
        String sql = conta.perfil() == Perfil.ACOMPANHADO
                ? "SELECT CD_PESSOA_ACOMPANHADA FROM TB_PESSOA_ACOMPANHADA WHERE CD_USUARIO = ?"
                : VINCULOS_ATIVOS + " AND TP_PERMISSAO IN ('VISUALIZAR', 'REGISTRAR')";
        try {
            return new HashSet<>(jdbc.queryForList(sql, Long.class, conta.id()));
        } catch (DataAccessException e) {
            throw new PersistenciaException(e);
        }
    }

    public boolean podeRegistrar(long usuarioId, long pessoaId) {
        String sql = VINCULOS_ATIVOS + " AND CD_PESSOA_ACOMPANHADA = ? AND TP_PERMISSAO = 'REGISTRAR'";
        try {
            return !jdbc.queryForList(sql, Long.class, usuarioId, pessoaId).isEmpty();
        } catch (DataAccessException e) {
            throw new PersistenciaException(e);
        }
    }

    static void inserirFamiliar(Connection connection, long familiarId, long pessoaId) throws SQLException {
        try (var vinculo = connection.prepareStatement("""
                INSERT INTO TB_VINCULO (CD_VINCULO, CD_USUARIO, CD_PESSOA_ACOMPANHADA,
                    TP_RELACAO, TP_PERMISSAO, ST_AUTORIZACAO, DT_VINCULO)
                VALUES (SQ_TB_VINCULO.NEXTVAL, ?, ?, 'FAMILIAR', 'REGISTRAR', 'ATIVO', SYSDATE)
                """)) {
            vinculo.setLong(1, familiarId);
            vinculo.setLong(2, pessoaId);
            vinculo.executeUpdate();
        }
    }

    public List<VinculoProfissional> listarProfissionais(long pessoaId) {
        return listarProfissionais(" AND v.CD_PESSOA_ACOMPANHADA = ?", pessoaId);
    }

    public List<VinculoProfissional> listarDoProfissional(long contaId) {
        return listarProfissionais(" AND v.CD_USUARIO = ?", contaId);
    }

    private List<VinculoProfissional> listarProfissionais(String filtro, long id) {
        try {
            return jdbc.query(PROFISSIONAIS + filtro + " ORDER BY v.CD_VINCULO",
                    (row, numero) -> new VinculoProfissional(row.getLong("CD_VINCULO"),
                            row.getLong("CD_PESSOA_ACOMPANHADA"), row.getLong("CD_USUARIO"),
                            row.getString("NM_USUARIO"), Permissao.valueOf(row.getString("TP_PERMISSAO")),
                            row.getString("ST_AUTORIZACAO")), id);
        } catch (DataAccessException e) {
            throw new PersistenciaException(e);
        }
    }

    @Transactional
    public boolean convidarProfissional(long pessoaId, long profissionalId, Permissao permissao) {
        try {
            // Serializa convites para a mesma pessoa sem exigir UNIQUE no DDL.
            if (jdbc.queryForList("""
                    SELECT CD_PESSOA_ACOMPANHADA FROM TB_PESSOA_ACOMPANHADA
                    WHERE CD_PESSOA_ACOMPANHADA = ? FOR UPDATE
                    """, Long.class, pessoaId).isEmpty()) {
                throw new PessoaAcompanhadaNaoEncontradaException();
            }
            if (existe("""
                    SELECT COUNT(*) FROM TB_VINCULO
                    WHERE CD_USUARIO = ? AND CD_PESSOA_ACOMPANHADA = ? AND ST_AUTORIZACAO <> 'REVOGADO'
                    """, profissionalId, pessoaId)) {
                return false;
            }
            return executar("""
                    INSERT INTO TB_VINCULO (CD_VINCULO, CD_USUARIO, CD_PESSOA_ACOMPANHADA,
                        TP_RELACAO, TP_PERMISSAO, ST_AUTORIZACAO, DT_VINCULO)
                    VALUES (SQ_TB_VINCULO.NEXTVAL, ?, ?, 'PROFISSIONAL', ?, 'PENDENTE', SYSDATE)
                    """, profissionalId, pessoaId, permissao.name());
        } catch (DataAccessException e) {
            throw new PersistenciaException(e);
        }
    }

    public boolean alterarPermissao(long pessoaId, long vinculoId, Permissao permissao) {
        return executar("""
                UPDATE TB_VINCULO SET TP_PERMISSAO = ?
                WHERE CD_PESSOA_ACOMPANHADA = ? AND CD_VINCULO = ?
                  AND TP_RELACAO = 'PROFISSIONAL' AND ST_AUTORIZACAO IN ('PENDENTE', 'ATIVO')
                """, permissao.name(), pessoaId, vinculoId);
    }

    public boolean revogarProfissional(long pessoaId, long vinculoId) {
        return executar("""
                UPDATE TB_VINCULO SET ST_AUTORIZACAO = 'REVOGADO'
                WHERE CD_PESSOA_ACOMPANHADA = ? AND CD_VINCULO = ?
                  AND TP_RELACAO = 'PROFISSIONAL' AND ST_AUTORIZACAO IN ('PENDENTE', 'ATIVO')
                """, pessoaId, vinculoId);
    }

    public boolean aceitarConvite(long profissionalId, long vinculoId) {
        return executar("""
                UPDATE TB_VINCULO SET ST_AUTORIZACAO = 'ATIVO'
                WHERE CD_USUARIO = ? AND CD_VINCULO = ?
                  AND TP_RELACAO = 'PROFISSIONAL' AND ST_AUTORIZACAO = 'PENDENTE'
                """, profissionalId, vinculoId);
    }

    private boolean executar(String sql, Object... parametros) {
        try {
            return jdbc.update(sql, parametros) > 0;
        } catch (DataAccessException e) {
            throw new PersistenciaException(e);
        }
    }

    public boolean referenciasDaPessoa(long pessoaId, Long medicamentoId, Long consultaId) {
        try {
            return (medicamentoId == null || existe("""
                    SELECT COUNT(*) FROM TB_MEDICAMENTO
                    WHERE CD_MEDICAMENTO = ? AND CD_PESSOA_ACOMPANHADA = ?
                    """, medicamentoId, pessoaId))
                    && (consultaId == null || existe("""
                    SELECT COUNT(*) FROM TB_CONSULTA
                    WHERE CD_CONSULTA = ? AND CD_PESSOA_ACOMPANHADA = ?
                    """, consultaId, pessoaId));
        } catch (DataAccessException e) {
            throw new PersistenciaException(e);
        }
    }

    private boolean existe(String sql, long referencia, long pessoa) {
        return jdbc.queryForObject(sql, Long.class, referencia, pessoa) > 0;
    }
}
