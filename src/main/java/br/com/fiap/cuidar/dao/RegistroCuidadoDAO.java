package br.com.fiap.cuidar.dao;
import br.com.fiap.cuidar.models.RegistroCuidado;
import br.com.fiap.cuidar.exception.*;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
@Repository
public class RegistroCuidadoDAO {
    public boolean areaHabilitada(Long pessoaId, Long areaId) {
        String sql = "SELECT 1 FROM TB_PESSOA_AREA_CUIDADO WHERE CD_PESSOA_ACOMPANHADA = ? AND CD_AREA = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, pessoaId);
            s.setLong(2, areaId);
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        } catch (SQLException e) { throw traduzir(e); }
    }
    private final DataSource dataSource;
    public RegistroCuidadoDAO(DataSource dataSource) { this.dataSource = dataSource; }
    private static final String COLUNAS = "CD_REGISTRO, CD_PESSOA_ACOMPANHADA, CD_AREA, CD_USUARIO_CRIOU, CD_USUARIO_REALIZOU, CD_MEDICAMENTO, CD_CONSULTA, DS_TITULO, DS_DETALHE, DT_PREVISTA, DT_REALIZADO, ST_REGISTRO, VL_MEDIDA, VL_MEDIDA_2, DS_UNIDADE";
    public RegistroCuidado cadastrar(RegistroCuidado registro) {
        String sql = "INSERT INTO TB_REGISTRO_CUIDADO (" + COLUNAS + ") VALUES (SQ_TB_REGISTRO_CUIDADO.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql, new String[]{"CD_REGISTRO"})) {
            preencher(s, registro); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new PersistenciaException(new IllegalStateException("ID n?o retornado."));
                registro.setId(keys.getLong(1));
            }
            return registro;
        } catch (SQLException e) { throw traduzir(e); }
    }
    public List<RegistroCuidado> listar() { return consultar(null); }
    public List<RegistroCuidado> listarPorPessoa(Long pessoaId) { return consultar(pessoaId); }
    private List<RegistroCuidado> consultar(Long pessoaId) {
        String sql = "SELECT " + COLUNAS + " FROM TB_REGISTRO_CUIDADO" + (pessoaId == null ? "" : " WHERE CD_PESSOA_ACOMPANHADA = ?") + " ORDER BY CD_REGISTRO";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            if (pessoaId != null) s.setLong(1, pessoaId);
            try (ResultSet r = s.executeQuery()) {
                List<RegistroCuidado> lista = new ArrayList<>();
                while (r.next()) lista.add(ler(r));
                return lista;
            }
        } catch (SQLException e) { throw traduzir(e); }
    }
    public Optional<RegistroCuidado> buscarPorId(Long id) {
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("SELECT " + COLUNAS + " FROM TB_REGISTRO_CUIDADO WHERE CD_REGISTRO = ?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(ler(r)) : Optional.empty(); }
        } catch (SQLException e) { throw traduzir(e); }
    }
    public boolean atualizar(RegistroCuidado registro) {
        String sql = "UPDATE TB_REGISTRO_CUIDADO SET CD_PESSOA_ACOMPANHADA = ?, CD_AREA = ?, CD_USUARIO_CRIOU = ?, CD_USUARIO_REALIZOU = ?, CD_MEDICAMENTO = ?, CD_CONSULTA = ?, DS_TITULO = ?, DS_DETALHE = ?, DT_PREVISTA = ?, DT_REALIZADO = ?, ST_REGISTRO = ?, VL_MEDIDA = ?, VL_MEDIDA_2 = ?, DS_UNIDADE = ? WHERE CD_REGISTRO = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            preencher(s, registro); s.setLong(15, registro.getId()); return s.executeUpdate() > 0;
        } catch (SQLException e) { throw traduzir(e); }
    }
    public boolean excluir(Long id) {
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("DELETE FROM TB_REGISTRO_CUIDADO WHERE CD_REGISTRO = ?")) {
            s.setLong(1, id); return s.executeUpdate() > 0;
        } catch (SQLException e) { throw traduzir(e); }
    }
    private void preencher(PreparedStatement s, RegistroCuidado registro) throws SQLException {
        s.setObject(1, registro.getPessoaAcompanhadaId(), Types.NUMERIC);
        s.setObject(2, registro.getAreaId(), Types.NUMERIC);
        s.setObject(3, registro.getUsuarioCriouId(), Types.NUMERIC);
        s.setObject(4, registro.getUsuarioRealizouId(), Types.NUMERIC);
        s.setObject(5, registro.getMedicamentoId(), Types.NUMERIC);
        s.setObject(6, registro.getConsultaId(), Types.NUMERIC);
        s.setObject(7, registro.getTitulo(), Types.VARCHAR);
        s.setObject(8, registro.getDetalhe(), Types.VARCHAR);
        if (registro.getDataPrevista() == null) s.setNull(9, Types.TIMESTAMP); else s.setTimestamp(9, Timestamp.valueOf(registro.getDataPrevista()));
        if (registro.getDataRealizado() == null) s.setNull(10, Types.TIMESTAMP); else s.setTimestamp(10, Timestamp.valueOf(registro.getDataRealizado()));
        s.setObject(11, registro.getStatus(), Types.VARCHAR);
        s.setObject(12, registro.getMedida(), Types.NUMERIC);
        s.setObject(13, registro.getMedida2(), Types.NUMERIC);
        s.setObject(14, registro.getUnidade(), Types.VARCHAR);
    }
    private RegistroCuidado ler(ResultSet r) throws SQLException {
        RegistroCuidado registro = new RegistroCuidado();
        registro.setId(r.getLong("CD_REGISTRO"));
        registro.setPessoaAcompanhadaId(r.getObject("CD_PESSOA_ACOMPANHADA", Long.class));
        registro.setAreaId(r.getObject("CD_AREA", Long.class));
        registro.setUsuarioCriouId(r.getObject("CD_USUARIO_CRIOU", Long.class));
        registro.setUsuarioRealizouId(r.getObject("CD_USUARIO_REALIZOU", Long.class));
        registro.setMedicamentoId(r.getObject("CD_MEDICAMENTO", Long.class));
        registro.setConsultaId(r.getObject("CD_CONSULTA", Long.class));
        registro.setTitulo(r.getString("DS_TITULO"));
        registro.setDetalhe(r.getString("DS_DETALHE"));
        Timestamp dataPrevista = r.getTimestamp("DT_PREVISTA");
        registro.setDataPrevista(dataPrevista == null ? null : dataPrevista.toLocalDateTime());
        Timestamp dataRealizado = r.getTimestamp("DT_REALIZADO");
        registro.setDataRealizado(dataRealizado == null ? null : dataRealizado.toLocalDateTime());
        registro.setStatus(r.getString("ST_REGISTRO"));
        registro.setMedida(r.getBigDecimal("VL_MEDIDA"));
        registro.setMedida2(r.getBigDecimal("VL_MEDIDA_2"));
        registro.setUnidade(r.getString("DS_UNIDADE"));
        return registro;
    }
    private RuntimeException traduzir(SQLException e) {
        for (SQLException atual = e; atual != null; atual = atual.getNextException()) {
            if (atual.getErrorCode() == 2291) return new ConflitoException("Uma refer?ncia informada n?o existe.", e);
            if (atual.getErrorCode() == 1 || atual.getErrorCode() == 2292) return new ConflitoException("O registro conflita com dados relacionados.", e);
        }
        return new PersistenciaException(e);
    }
}
