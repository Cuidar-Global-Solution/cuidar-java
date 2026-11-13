package br.com.fiap.cuidar;

import br.com.fiap.cuidar.dao.PessoaIdosaDAO;
import br.com.fiap.cuidar.exception.ConflitoException;
import br.com.fiap.cuidar.models.PessoaIdosa;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PessoaIdosaDAOTests {
    @Test
    void gravarNulosRecuperarIdEFecharRecursos() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet keys = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(contains("SQ_TB_PESSOA_IDOSA.NEXTVAL"), aryEqColuna())).thenReturn(statement);
        when(statement.getGeneratedKeys()).thenReturn(keys);
        when(keys.next()).thenReturn(true);
        when(keys.getLong(1)).thenReturn(55L);
        var pessoa = new PessoaIdosa();
        pessoa.setNome("Maria");
        pessoa.setDataNascimento(LocalDate.of(1950, 1, 2));
        pessoa.setTamanhoFonte(18);
        pessoa.setAltoContraste("N");
        assertEquals(55L, new PessoaIdosaDAO(ds).cadastrar(pessoa).getId());
        verify(statement).setNull(1, Types.NUMERIC);
        verify(statement).setNull(4, Types.VARCHAR);
        verify(keys).close();
        verify(statement).close();
        verify(connection).close();
    }

    private String[] aryEqColuna() {
        return org.mockito.AdditionalMatchers.aryEq(new String[]{"CD_PESSOA_IDOSA"});
    }

    @Test
    void lerUsuarioNuloSemTransformarEmZero() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getLong("CD_USUARIO")).thenReturn(0L);
        when(result.wasNull()).thenReturn(true);
        when(result.getDate("DT_NASCIMENTO")).thenReturn(Date.valueOf("1950-01-02"));
        PessoaIdosa pessoa = new PessoaIdosaDAO(ds).buscarPorId(1L).orElseThrow();
        assertNull(pessoa.getUsuarioId());
        assertNull(pessoa.getObservacao());
        verify(result).close();
        verify(connection).close();
    }

    @Test
    void restricoesOracleViraramConflitoERecursosFecham() throws Exception {
        for (int codigo : new int[]{1, 2291, 2292}) {
            DataSource ds = mock(DataSource.class);
            Connection connection = mock(Connection.class);
            PreparedStatement statement = mock(PreparedStatement.class);
            when(ds.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeUpdate()).thenThrow(new SQLException("SQL privado", "23000", codigo));
            assertThrows(ConflitoException.class, () -> new PessoaIdosaDAO(ds).excluir(1L));
            verify(statement).close();
            verify(connection).close();
        }
    }
}
