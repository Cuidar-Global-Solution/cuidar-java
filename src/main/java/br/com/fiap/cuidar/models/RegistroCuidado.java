package br.com.fiap.cuidar.models;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class RegistroCuidado {
    private Long id;
    private Long pessoaAcompanhadaId;
    private Long areaId;
    private Long usuarioCriouId;
    private Long usuarioRealizouId;
    private Long medicamentoId;
    private Long consultaId;
    private String titulo;
    private String detalhe;
    private LocalDateTime dataPrevista;
    private LocalDateTime dataRealizado;
    private String status;
    private BigDecimal medida;
    private BigDecimal medida2;
    private String unidade;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPessoaAcompanhadaId() { return pessoaAcompanhadaId; }
    public void setPessoaAcompanhadaId(Long pessoaAcompanhadaId) { this.pessoaAcompanhadaId = pessoaAcompanhadaId; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public Long getUsuarioCriouId() { return usuarioCriouId; }
    public void setUsuarioCriouId(Long usuarioCriouId) { this.usuarioCriouId = usuarioCriouId; }
    public Long getUsuarioRealizouId() { return usuarioRealizouId; }
    public void setUsuarioRealizouId(Long usuarioRealizouId) { this.usuarioRealizouId = usuarioRealizouId; }
    public Long getMedicamentoId() { return medicamentoId; }
    public void setMedicamentoId(Long medicamentoId) { this.medicamentoId = medicamentoId; }
    public Long getConsultaId() { return consultaId; }
    public void setConsultaId(Long consultaId) { this.consultaId = consultaId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDetalhe() { return detalhe; }
    public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
    public LocalDateTime getDataPrevista() { return dataPrevista; }
    public void setDataPrevista(LocalDateTime dataPrevista) { this.dataPrevista = dataPrevista; }
    public LocalDateTime getDataRealizado() { return dataRealizado; }
    public void setDataRealizado(LocalDateTime dataRealizado) { this.dataRealizado = dataRealizado; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getMedida() { return medida; }
    public void setMedida(BigDecimal medida) { this.medida = medida; }
    public BigDecimal getMedida2() { return medida2; }
    public void setMedida2(BigDecimal medida2) { this.medida2 = medida2; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
}
