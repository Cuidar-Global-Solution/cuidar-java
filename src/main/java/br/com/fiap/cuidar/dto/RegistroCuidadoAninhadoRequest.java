package br.com.fiap.cuidar.dto;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public class RegistroCuidadoAninhadoRequest {
    @NotNull
    @Positive
    @Max(999)
    private Long areaId;
    @NotNull
    @Positive
    @Max(9999999999L)
    private Long usuarioCriouId;
    @Positive
    @Max(9999999999L)
    private Long usuarioRealizouId;
    @Positive
    @Max(9999999999L)
    private Long medicamentoId;
    @Positive
    @Max(9999999999L)
    private Long consultaId;
    @NotBlank
    @Size(max = 100)
    private String titulo;
    @Size(max = 300)
    private String detalhe;
    private LocalDateTime dataPrevista;
    private LocalDateTime dataRealizado;
    @NotNull
    @Pattern(regexp = "PENDENTE|TOMADA|NAO_TOMADA|SEM_CONFIRMACAO|CONCLUIDO")
    private String status;
    @Digits(integer = 6, fraction = 2)
    private BigDecimal medida;
    @Digits(integer = 6, fraction = 2)
    private BigDecimal medida2;
    @Size(max = 20)
    private String unidade;
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
    @AssertTrue(message = "Informe dataPrevista ou dataRealizado.")
    public boolean isDataInformada() { return dataPrevista != null || dataRealizado != null; }
}
