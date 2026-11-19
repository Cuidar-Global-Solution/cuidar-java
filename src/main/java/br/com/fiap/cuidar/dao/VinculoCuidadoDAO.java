package br.com.fiap.cuidar.dao;

import br.com.fiap.cuidar.exception.PersistenciaException;
import br.com.fiap.cuidar.models.Conta;
import br.com.fiap.cuidar.models.Conta.Perfil;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.Set;

@Repository
public class VinculoCuidadoDAO {
    private final JdbcTemplate jdbc;

    public VinculoCuidadoDAO(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
    }

    public Set<Long> pessoasPermitidas(Conta conta) {
        String sql = conta.perfil() == Perfil.ACOMPANHADO
                ? "SELECT CD_PESSOA_ACOMPANHADA FROM TB_PESSOA_ACOMPANHADA WHERE CD_USUARIO = ?"
                : "SELECT CD_PESSOA_ACOMPANHADA FROM TB_USUARIO_PESSOA_CUIDADO WHERE CD_USUARIO = ?";
        try {
            return new HashSet<>(jdbc.queryForList(sql, Long.class, conta.id()));
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
