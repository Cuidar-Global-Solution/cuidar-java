package br.com.fiap.cuidar.dao;

import br.com.fiap.cuidar.exception.PersistenciaException;
import br.com.fiap.cuidar.models.Conta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Optional;

@Repository
public class SessaoDAO {
    private final JdbcTemplate jdbc;

    public SessaoDAO(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public void criar(String hash, long contaId, long segundos) {
        try {
            jdbc.update("""
                    INSERT INTO TB_SESSAO (DS_TOKEN_HASH, CD_USUARIO, DT_EXPIRACAO)
                    VALUES (?, ?, SYSTIMESTAMP + NUMTODSINTERVAL(?, 'SECOND'))
                    """, hash, contaId, segundos);
        } catch (DataAccessException e) {
            throw new PersistenciaException(e);
        }
    }

    public Optional<Conta> buscarAtiva(String hash) {
        try {
            return jdbc.query("""
                    SELECT u.CD_USUARIO, u.NM_USUARIO, u.DS_EMAIL, u.TP_USUARIO
                    FROM TB_SESSAO s JOIN TB_USUARIO u ON u.CD_USUARIO = s.CD_USUARIO
                    WHERE s.DS_TOKEN_HASH = ? AND s.DT_EXPIRACAO > SYSTIMESTAMP
                    """, (row, index) -> ContaDAO.lerConta(row), hash).stream().findFirst();
        } catch (DataAccessException e) {
            throw new PersistenciaException(e);
        }
    }

    public void revogar(String hash) {
        try {
            jdbc.update("DELETE FROM TB_SESSAO WHERE DS_TOKEN_HASH = ?", hash);
        } catch (DataAccessException e) {
            throw new PersistenciaException(e);
        }
    }
}
