package br.com.fiap.cuidar.dao;

import br.com.fiap.cuidar.exception.ConflitoException;
import br.com.fiap.cuidar.exception.PersistenciaException;
import br.com.fiap.cuidar.models.Conta;
import br.com.fiap.cuidar.models.Conta.Perfil;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class ContaDAO {
    private final DataSource dataSource;

    public ContaDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Conta cadastrar(String nome, String email, String hash, Perfil perfil) {
        String sql = """
                INSERT INTO TB_USUARIO (
                    CD_USUARIO, NM_USUARIO, DS_EMAIL, DS_SENHA, TP_USUARIO,
                    TP_PLANO, TP_CANAL_LEMBRETE, TP_FREQUENCIA_RESUMO, DT_CADASTRO
                ) VALUES (SQ_TB_USUARIO.NEXTVAL, ?, ?, ?, ?, 'GRATUITO', 'EMAIL', 'NENHUM', SYSDATE)
                """;
        try (var conexao = dataSource.getConnection();
             var comando = conexao.prepareStatement(sql, new String[]{"CD_USUARIO"})) {
            comando.setString(1, nome);
            comando.setString(2, email);
            comando.setString(3, hash);
            comando.setString(4, perfil.name());
            comando.executeUpdate();
            try (var chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) throw new PersistenciaException(new IllegalStateException("ID da conta ausente."));
                return new Conta(chaves.getLong(1), nome, email, perfil);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1) {
                throw new ConflitoException("O e-mail informado já está cadastrado.", e);
            }
            throw new PersistenciaException(e);
        }
    }

    public Optional<Credencial> buscarCredencial(String email) {
        String sql = """
                SELECT CD_USUARIO, NM_USUARIO, DS_EMAIL, TP_USUARIO, DS_SENHA
                FROM TB_USUARIO WHERE LOWER(TRIM(DS_EMAIL)) = ?
                """;
        try (var conexao = dataSource.getConnection(); var comando = conexao.prepareStatement(sql)) {
            comando.setString(1, email);
            try (var dados = comando.executeQuery()) {
                return dados.next()
                        ? Optional.of(new Credencial(lerConta(dados), dados.getString("DS_SENHA")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException(e);
        }
    }

    static Conta lerConta(ResultSet dados) throws SQLException {
        return new Conta(dados.getLong("CD_USUARIO"), dados.getString("NM_USUARIO"),
                dados.getString("DS_EMAIL"), Perfil.valueOf(dados.getString("TP_USUARIO")));
    }

    public record Credencial(Conta conta, String hash) {
        @Override
        public String toString() {
            return "Credencial[contaId=" + conta.id() + "]";
        }
    }
}
