package br.com.fiap.cuidar;

import br.com.fiap.cuidar.dao.RegistroCuidadoDAO;
import br.com.fiap.cuidar.models.RegistroCuidado;
import br.com.fiap.cuidar.exception.ConflitoException;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RegistroCuidadoDAOTests {
    @Test
    void cadastrarUsaSequencePreservaHorarioNulosEFechaRecursos() throws Exception {
        DataSource ds = mock(DataSource.class); Connection c = mock(Connection.class);
        PreparedStatement s = mock(PreparedStatement.class); ResultSet keys = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(contains("SQ_TB_REGISTRO_CUIDADO.NEXTVAL"),
                org.mockito.AdditionalMatchers.aryEq(new String[]{"CD_REGISTRO"}))).thenReturn(s);
        when(s.getGeneratedKeys()).thenReturn(keys); when(keys.next()).thenReturn(true); when(keys.getLong(1)).thenReturn(9L);
        var r = new RegistroCuidado(); r.setPessoaAcompanhadaId(3L); r.setAreaId(1L); r.setUsuarioCriouId(2L);
        r.setTitulo("Água"); r.setStatus("PENDENTE"); r.setDataPrevista(LocalDateTime.of(2026,11,20,10,30));
        assertEquals(9L, new RegistroCuidadoDAO(ds).cadastrar(r).getId());
        verify(s).setTimestamp(9, Timestamp.valueOf(r.getDataPrevista()));
        verify(s).setNull(10, Types.TIMESTAMP);
        verify(s).setObject(4, null, Types.NUMERIC);
        verify(keys).close(); verify(s).close(); verify(c).close();
    }

    @Test
    void listarPorPessoaFiltraFkEMapeiaNulosSemVirarZero() throws Exception {
        DataSource ds = mock(DataSource.class); Connection c = mock(Connection.class);
        PreparedStatement s = mock(PreparedStatement.class); ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(contains("WHERE CD_PESSOA_ACOMPANHADA = ?"))).thenReturn(s);
        when(s.executeQuery()).thenReturn(rs); when(rs.next()).thenReturn(true, false);
        when(rs.getLong("CD_REGISTRO")).thenReturn(9L);
        when(rs.getObject("CD_PESSOA_ACOMPANHADA", Long.class)).thenReturn(3L);
        when(rs.getTimestamp("DT_PREVISTA")).thenReturn(Timestamp.valueOf("2026-11-20 10:30:00"));
        var registros = new RegistroCuidadoDAO(ds).listarPorPessoa(3L);
        assertEquals(1, registros.size()); assertNull(registros.getFirst().getMedicamentoId());
        assertEquals(3L, registros.getFirst().getPessoaAcompanhadaId());
        assertEquals(LocalDateTime.of(2026,11,20,10,30), registros.getFirst().getDataPrevista());
        verify(s).setLong(1, 3L); verify(rs).close(); verify(s).close(); verify(c).close();
    }

    @Test
    void fkInexistenteViraConflitoEFechaRecursos() throws Exception {
        DataSource ds = mock(DataSource.class); Connection c = mock(Connection.class); PreparedStatement s = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(c); when(c.prepareStatement(anyString())).thenReturn(s);
        when(s.executeUpdate()).thenThrow(new SQLException("SQL privado", "23000", 2291));
        assertThrows(ConflitoException.class, () -> new RegistroCuidadoDAO(ds).atualizar(new RegistroCuidadoComId()));
        verify(s).close(); verify(c).close();
    }

    private static class RegistroCuidadoComId extends RegistroCuidado {
        RegistroCuidadoComId() { setId(9L); }
    }
}
